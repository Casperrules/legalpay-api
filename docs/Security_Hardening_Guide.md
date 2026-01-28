# Security Hardening Guide - LegalPay Platform

## Executive Summary

This document outlines the security posture for LegalPay's contract-to-cash automation platform, addressing **containerization vulnerabilities** and establishing defense-in-depth principles for PCI DSS and RBI compliance.

---

## 1. Container Security (Production Only)

**Local Development**: Native services (PostgreSQL, Redis, RabbitMQ, Node.js) - no containers needed.
**Production**: Kubernetes with gVisor sandboxed runtime for PCI DSS compliance.

### 1.1 Why No Containers for Local Dev?

**Disk Space**: Docker/Podman images consume 10-50GB (base images + layers)
**Performance**: Native services faster on macOS (no VM overhead)
**Simplicity**: Fewer moving parts for debugging

### 1.2 Why Containers ONLY in Production?

**Known Vulnerabilities:**

- **CVE-2024-21626**: runC process.cwd escape (CVSS 8.6)
- **CVE-2024-23651**: BuildKit mount cache race condition
- **CVE-2023-28842**: containerd privilege escalation
- Docker daemon runs as **root** (systemd `docker.service` requires CAP_SYS_ADMIN)

**Attack Surface:**

```
User Process → Docker CLI → Docker Daemon (root) → containerd → runC → Kernel
                              ↑
                        Single point of failure
```

### 1.2 Recommended Alternatives

| Solution             | Security Model          | Use Case                | PCI DSS Compliance           |
| -------------------- | ----------------------- | ----------------------- | ---------------------------- |
| **Podman**           | Daemonless, rootless    | Local dev, CI/CD        | ✅ Reduces attack surface    |
| **gVisor**           | User-space kernel       | Production (GKE/EKS)    | ✅ Syscall interception      |
| **Kata Containers**  | Hardware virtualization | High-security workloads | ✅ VM-level isolation        |
| **Systemd Services** | Native Linux security   | Legacy migration        | ⚠️ Requires manual hardening |

---

## 2. Production Deployment - gVisor on Kubernetes

### 2.1 Architecture

```
Pod → gVisor Sandbox (runsc) → Limited Syscalls → Host Kernel
      ↑
   User-space kernel intercepts:
   - open(), read(), write()
   - socket(), bind(), connect()
   - fork(), execve()
```

**Benefits:**

- Prevents container escape (no direct kernel access)
- Limits blast radius of kernel exploits
- Compatible with existing OCI images

### 2.2 GKE Setup

```bash
# Enable GKE Sandbox (gVisor)
gcloud container clusters create legalpay-prod \
  --enable-sandbox \
  --sandbox-type=gvisor \
  --machine-type=n2-standard-4 \
  --num-nodes=3 \
  --region=asia-south1 \
  --release-channel=regular \
  --enable-ip-alias \
  --enable-private-nodes \
  --master-ipv4-cidr=172.16.0.0/28

# Verify RuntimeClass
kubectl get runtimeclass
# NAME     HANDLER   AGE
# gvisor   runsc     1m
```

### 2.3 Deployment Configuration

**Critical Security Settings:**

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: payment-service
  annotations:
    # PCI DSS v4.0 Requirement 2.2.4
    compliance.pci-dss: "2.2.4"
spec:
  template:
    spec:
      runtimeClassName: gvisor # ⚠️ MANDATORY for payment services

      # Pod-level security
      securityContext:
        runAsNonRoot: true
        runAsUser: 10001
        fsGroup: 10001
        seccompProfile:
          type: RuntimeDefault

      # Container-level hardening
      containers:
        - name: api
          securityContext:
            allowPrivilegeEscalation: false
            readOnlyRootFilesystem: true
            capabilities:
              drop: [ALL]

          # Resource limits (prevent DoS)
          resources:
            requests:
              memory: "512Mi"
              cpu: "500m"
            limits:
              memory: "1Gi"
              cpu: "1000m"

          # Volume mounts (minimal)
          volumeMounts:
            - name: tmp
              mountPath: /tmp
            - name: cache
              mountPath: /app/.cache

      volumes:
        - name: tmp
          emptyDir: {}
        - name: cache
          emptyDir: {}
```

### 2.4 Network Policies

```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: payment-service-isolation
spec:
  podSelector:
    matchLabels:
      app: payment-service
  policyTypes:
    - Ingress
    - Egress

  ingress:
    - from:
        - podSelector:
            matchLabels:
              app: api-gateway
      ports:
        - protocol: TCP
          port: 8080

  egress:
    # Allow Razorpay API (explicit whitelist)
    - to:
        - namespaceSelector: {}
          podSelector:
            matchLabels:
              app: kube-dns
      ports:
        - protocol: UDP
          port: 53
    - to:
        - podSelector:
            matchLabels:
              app: postgres
      ports:
        - protocol: TCP
          port: 5432
  # Block all other egress
```

---

## 3. Local Development Security (Native Services)

## 3. Local Development Security (Native Services)

### 3.1 Database Security

**PostgreSQL Hardening** (`postgresql.conf`):

```ini
# Listen only on localhost
listen_addresses = 'localhost'

# Limit connections
max_connections = 20

# SSL/TLS for local connections (dev)
ssl = off  # Enable in staging/prod

# Logging
log_statement = 'mod'  # Log DDL/DML
log_duration = on
log_min_duration_statement = 1000  # Log slow queries > 1s
```

**pg_hba.conf** (client authentication):

```
# TYPE  DATABASE        USER            ADDRESS                 METHOD
local   legalpay        legalpay                                scram-sha-256
host    legalpay        legalpay        127.0.0.1/32            scram-sha-256
host    legalpay        legalpay        ::1/128                 scram-sha-256

# Deny all other connections
host    all             all             0.0.0.0/0               reject
```

### 3.2 Redis Security

**redis.conf**:

```ini
# Bind to localhost only
bind 127.0.0.1 ::1

# Require password
requirepass dev_secure_password_change_in_prod

# Disable dangerous commands
rename-command FLUSHDB ""
rename-command FLUSHALL ""
rename-command CONFIG "CONFIG_abc123"

# Enable AOF persistence
appendonly yes
appendfsync everysec
```

### 3.3 RabbitMQ Security

**rabbitmq.conf**:

```ini
# Disable guest user remote access
loopback_users.guest = true

# Enable management plugin with auth
management.load_definitions = /etc/rabbitmq/definitions.json
```

**definitions.json** (user/vhost setup):

```json
{
  "users": [
    {
      "name": "legalpay",
      "password_hash": "...",
      "tags": "administrator"
    }
  ],
  "vhosts": [{ "name": "legalpay_vhost" }],
  "permissions": [
    {
      "user": "legalpay",
      "vhost": "legalpay_vhost",
      "configure": ".*",
      "write": ".*",
      "read": ".*"
    }
  ]
}
```

### 3.4 Application Security

**Environment Variable Isolation**:

```bash
# Use direnv for automatic .env loading
brew install direnv
echo 'eval "$(direnv hook zsh)"' >> ~/.zshrc

# Create .envrc (auto-loads when entering directory)
cat > .envrc <<EOF
export DATABASE_URL=postgresql://legalpay:dev_password@localhost:5432/legalpay
export REDIS_URL=redis://:dev_secure_password@localhost:6379
export RABBITMQ_URL=amqp://legalpay:dev_password@localhost:5672/legalpay_vhost
EOF

direnv allow .
```

**File Permissions**:

```bash
# Protect .env files
chmod 600 .env.local .envrc

# Protect SSH keys / certificates
chmod 600 ~/.ssh/legalpay_rsa

# Audit file permissions
find . -type f -name "*.key" -o -name "*.pem" | xargs ls -l
```

### 3.5 Network Firewall

**macOS:**

```bash
# Enable macOS firewall
sudo /usr/libexec/ApplicationFirewall/socketfilterfw --setglobalstate on

# Block incoming connections (except localhost)
sudo /usr/libexec/ApplicationFirewall/socketfilterfw --setblockall on
```

**Linux (UFW):**

```bash
# Install UFW
sudo apt install ufw

# Default deny incoming
sudo ufw default deny incoming
sudo ufw default allow outgoing

# Allow SSH (if needed)
sudo ufw allow 22/tcp

# Enable firewall
sudo ufw enable

# Verify status
sudo ufw status verbose
```

### 3.6 Development Best Practices

**Secrets Management:**

```bash
# NEVER commit secrets to Git
echo ".env*" >> .gitignore
echo "*.key" >> .gitignore
echo "*.pem" >> .gitignore

# Use git-secrets to prevent accidental commits
brew install git-secrets
git secrets --install
git secrets --register-aws  # Detects AWS keys
git secrets --add 'razorpay_key_[a-zA-Z0-9]{24}'  # Custom pattern
```

**Dependency Scanning:**

```bash
# Audit npm dependencies weekly
npm audit
npm audit fix

# Use Snyk for vulnerability scanning
npx snyk test
```

---

## 4. Alternative: Systemd Services (No Containers)

### 4.1 When to Use

**Use systemd if:**

- ✅ Team lacks Kubernetes expertise
- ✅ Deploying to bare metal/VMs
- ✅ Legacy infrastructure requirements

**Security Trade-offs:**

- ❌ No isolation between services
- ❌ Manual dependency management
- ⚠️ Requires strict AppArmor/SELinux policies

### 4.2 Example Configuration

```ini
# /etc/systemd/system/legalpay-api.service
[Unit]
Description=LegalPay API Service
After=network.target postgresql.service

[Service]
Type=simple
User=legalpay
Group=legalpay
WorkingDirectory=/opt/legalpay/api

# Security hardening
NoNewPrivileges=true
PrivateTmp=true
ProtectSystem=strict
ProtectHome=true
ReadWritePaths=/var/log/legalpay /var/lib/legalpay

# Resource limits
MemoryLimit=1G
CPUQuota=100%

# Start command
ExecStart=/usr/bin/node /opt/legalpay/api/dist/server.js
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
```

**AppArmor Profile:**

```
#include <tunables/global>

/opt/legalpay/api/dist/server.js {
  #include <abstractions/base>
  #include <abstractions/nameservice>

  # Allow network
  network inet stream,
  network inet6 stream,

  # Read-only access to app
  /opt/legalpay/api/** r,

  # Write to logs
  /var/log/legalpay/** w,

  # Deny everything else
  deny /etc/shadow r,
  deny /root/** rwx,
}
```

---

## 5. Image Security

### 5.1 Base Image Selection

**Recommended:**

```dockerfile
# ✅ Use distroless (no shell, minimal attack surface)
FROM gcr.io/distroless/nodejs20-debian12

# ❌ Avoid full OS images
# FROM ubuntu:22.04  # Has 100+ vulnerabilities
```

**Vulnerability Scan:**

```bash
# Scan before pushing
grype legalpay/api:latest
trivy image legalpay/api:latest --severity HIGH,CRITICAL
```

### 5.2 Multi-stage Build

```dockerfile
# Stage 1: Build
FROM node:20-alpine AS builder
WORKDIR /build
COPY package*.json ./
RUN npm ci --only=production
COPY . .
RUN npm run build

# Stage 2: Runtime (distroless)
FROM gcr.io/distroless/nodejs20-debian12
COPY --from=builder /build/dist /app
COPY --from=builder /build/node_modules /app/node_modules
WORKDIR /app
USER nonroot:nonroot
CMD ["server.js"]
```

### 5.3 Supply Chain Security

**SLSA Build Provenance:**

```yaml
# .github/workflows/build.yml
name: Build with SLSA
on: push

jobs:
  build:
    runs-on: ubuntu-latest
    permissions:
      id-token: write
      contents: read

    steps:
      - uses: actions/checkout@v4

      - name: Build image
        run: podman build -t legalpay/api:${{ github.sha }} .

      - name: Generate SBOM
        run: syft legalpay/api:${{ github.sha }} -o cyclonedx-json > sbom.json

      - name: Sign image
        run: |
          cosign sign --key cosign.key legalpay/api:${{ github.sha }}
```

---

## 6. Compliance Checklist

### 6.1 PCI DSS v4.0 Requirements

| Requirement | Control                        | Implementation                       |
| ----------- | ------------------------------ | ------------------------------------ |
| **2.2.4**   | Secure system configurations   | gVisor RuntimeClass, securityContext |
| **2.2.5**   | Inventory of system components | SBOM generation with Syft            |
| **6.2.1**   | Vulnerability management       | Trivy scanning in CI/CD              |
| **8.2.1**   | Strong authentication          | JWT with refresh token rotation      |
| **10.2.1**  | Audit logging                  | Structured JSON logs to CloudWatch   |
| **12.3.1**  | Data usage policies            | PII redaction in logs                |

### 6.2 RBI Cybersecurity Framework

| Guideline                                           | Control                                        |
| --------------------------------------------------- | ---------------------------------------------- |
| IT Framework (2016)                                 | Encryption at rest (KMS), in transit (TLS 1.3) |
| Master Direction on Digital Payment Security (2022) | Webhook HMAC validation, idempotency keys      |
| Master Direction on Outsourcing (2023)              | Vendor risk assessment for Razorpay/Cashfree   |

---

## 7. Monitoring & Incident Response

### 7.1 Runtime Security Monitoring

**Falco Rules (Syscall Monitoring):**

```yaml
- rule: Unauthorized Process in Payment Container
  desc: Detect unexpected process execution
  condition: >
    container.image.repository = "legalpay/payment-service" and
    proc.name != "node" and
    proc.name != "health-check"
  output: "Unauthorized process in payment container (proc=%proc.name user=%user.name)"
  priority: CRITICAL
```

**Deployment:**

```bash
helm install falco falcosecurity/falco \
  --set falco.grpc.enabled=true \
  --set falco.grpcOutput.enabled=true
```

### 7.2 Incident Response Playbook

**Container Escape Detected:**

1. Isolate affected node: `kubectl cordon <node-name>`
2. Dump forensics: `kubectl debug node/<node> -it --image=ubuntu`
3. Terminate pod: `kubectl delete pod <pod-name> --force`
4. Rotate credentials: `kubectl delete secret db-creds && kubectl create secret ...`
5. Audit logs: `kubectl logs <pod> --previous > incident.log`

---

## 8. Summary Recommendations

### 8.1 Immediate Actions

| Priority  | Action                              | Timeline |
| --------- | ----------------------------------- | -------- |
| 🔴 **P0** | Replace Docker with Podman in CI/CD | 1 week   |
| 🔴 **P0** | Enable gVisor on production GKE     | 2 weeks  |
| 🟡 **P1** | Implement NetworkPolicies           | 1 week   |
| 🟡 **P1** | Deploy Falco for runtime monitoring | 2 weeks  |
| 🟢 **P2** | Sign container images with Cosign   | 1 month  |

### 8.2 Long-term Strategy

1. **Zero Trust Network**: Mutual TLS between all services (Istio/Linkerd)
2. **Immutable Infrastructure**: GitOps with sealed secrets (ArgoCD + Bitnami Sealed Secrets)
3. **Confidential Computing**: GKE Confidential VMs for PII processing
4. **Hardware Security Modules**: Cloud HSM for payment credential storage

---

## References

- [NIST SP 800-190: Application Container Security Guide](https://csrc.nist.gov/publications/detail/sp/800-190/final)
- [CIS Kubernetes Benchmark v1.8](https://www.cisecurity.org/benchmark/kubernetes)
- [PCI DSS v4.0 Cloud Computing Guidelines](https://www.pcisecuritystandards.org/documents/PCI_DSS_v4-0.pdf)
- [RBI Master Direction on Digital Payment Security Controls](https://www.rbi.org.in/Scripts/NotificationUser.aspx?Id=12404)
- [gVisor Security Model](https://gvisor.dev/docs/architecture_guide/security/)
- [Podman Rootless Containers](https://github.com/containers/podman/blob/main/docs/tutorials/rootless_tutorial.md)
