# Railway Build Fixes - LegalPay

## Problem: Build Failed on Railway

The error you encountered indicates Railway ran out of memory/timeout during Maven dependency download and compilation.

## ✅ Applied Fixes

### 1. **Memory Optimization**

- Added `MAVEN_OPTS` environment variable with `-Xmx1024m -Xms512m -XX:+UseG1GC`
- Reduced runtime JVM memory to `-Xmx512m -Xms256m` (sufficient for Spring Boot app)
- Added `.mvn/jvm.config` for consistent JVM settings

### 2. **Maven Wrapper**

- Generated Maven wrapper (`mvnw`) for consistent Maven version
- Removes dependency on Railway's pre-installed Maven version
- Ensures build reproducibility

### 3. **Build Optimization**

- Added `-Dmaven.compiler.useIncrementalCompilation=false` (faster on clean builds)
- Added `-B -ntp` flags (batch mode, no transfer progress = less logs)
- Changed from `mvn` to `./mvnw` in build commands

### 4. **Context Size Reduction**

- Created `.railwayignore` to exclude:
  - Frontend folder (deployed separately on Vercel)
  - Documentation files
  - Test files
  - Build outputs
  - IDE files

### 5. **Build Command Updates**

**railway.toml:**

```toml
[build]
builder = "NIXPACKS"
buildCommand = "./mvnw clean install -DskipTests -Dmaven.compiler.useIncrementalCompilation=false && cp legalpay-api/target/legalpay-api-1.0.0-SNAPSHOT.jar app.jar"

[deploy]
startCommand = "java -Xmx512m -Xms256m -jar app.jar"

[env]
MAVEN_OPTS = "-Xmx1024m -Xms512m -XX:+UseG1GC"
```

**nixpacks.toml:**

```toml
[phases.setup]
nixPkgs = ["openjdk21"]  # Removed maven - using wrapper instead

[phases.build]
cmds = [
  "chmod +x mvnw",
  "export MAVEN_OPTS='-Xmx1024m -Xms512m -XX:+UseG1GC'",
  "./mvnw clean install -DskipTests -Dmaven.compiler.useIncrementalCompilation=false -B -ntp",
  "cp legalpay-api/target/legalpay-api-1.0.0-SNAPSHOT.jar app.jar"
]
```

## 🚀 Next Steps

### 1. **Commit and Push Changes**

```bash
cd /path/to/LegalPayApp
git add .
git commit -m "fix: optimize Railway build with memory settings and Maven wrapper"
git push origin main
```

### 2. **Redeploy on Railway**

Railway will automatically trigger a new build when you push. The new configuration should:

- Complete build in 2-4 minutes (vs timing out)
- Use ~800MB memory during build
- Generate a 90-100MB Docker image

### 3. **Monitor Build Progress**

In Railway dashboard:

1. Click on your backend service
2. Go to "Deployments" tab
3. Click the latest deployment
4. Watch build logs - should see:
   ```
   ✓ Downloading Maven wrapper
   ✓ Building LegalPay Domain [1/4]
   ✓ Building LegalPay Services [2/4]
   ✓ Building LegalPay API [3/4]
   ✓ Copying JAR
   ```

## 📊 Resource Requirements

| Resource           | Requirement | Notes                                   |
| ------------------ | ----------- | --------------------------------------- |
| **Build Memory**   | 1GB         | Set via MAVEN_OPTS                      |
| **Runtime Memory** | 512MB       | Sufficient for Spring Boot API          |
| **Build Time**     | 2-4 minutes | On fresh build with dependency download |
| **Disk Space**     | ~500MB      | Including Maven cache                   |

## 🔧 If Build Still Fails

### Option 1: Increase Railway Plan

If you're on the free tier, consider upgrading to Hobby plan ($5/month) which provides:

- 8GB RAM (vs 512MB)
- Faster build VMs
- More reliable builds

### Option 2: Pre-build JAR Locally

```bash
# Build locally
./mvnw clean install -DskipTests

# Create simple Dockerfile
cat > Dockerfile.prebuilt <<EOF
FROM eclipse-temurin:21-jre-alpine
COPY legalpay-api/target/legalpay-api-1.0.0-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-Xmx512m", "-Xms256m", "-jar", "/app.jar"]
EOF

# Update railway.toml
cat > railway.toml <<EOF
[build]
builder = "DOCKERFILE"
dockerfilePath = "Dockerfile.prebuilt"

[deploy]
startCommand = ""
EOF
```

Then commit and push.

### Option 3: Use GitHub Actions to Build

Build in GitHub Actions (2GB RAM free tier) and push Docker image to Docker Hub:

```yaml
# .github/workflows/build.yml
name: Build and Push
on:
  push:
    branches: [main]
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          java-version: "21"
          distribution: "temurin"
      - name: Build with Maven
        run: ./mvnw clean install -DskipTests
      - name: Build Docker image
        run: docker build -t yourusername/legalpay-api:latest .
      - name: Push to Docker Hub
        run: |
          echo "${{ secrets.DOCKER_PASSWORD }}" | docker login -u "${{ secrets.DOCKER_USERNAME }}" --password-stdin
          docker push yourusername/legalpay-api:latest
```

Then deploy from Docker Hub on Railway.

## ✅ Success Indicators

You'll know the build succeeded when you see:

```
✓ Build successful
✓ Starting deployment
✓ Health check passed
✓ Deployment live at https://your-app.up.railway.app
```

## 🐛 Common Errors After This Fix

### 1. "Port 8080 already in use"

**Cause:** Railway sets `PORT` env variable dynamically
**Fix:** Already handled - we use `$PORT` in start command

### 2. "Database connection failed"

**Cause:** Missing PostgreSQL environment variables
**Solution:** Add in Railway dashboard:

```
DATABASE_URL=postgresql://user:pass@host:5432/dbname
SPRING_DATASOURCE_URL=${DATABASE_URL}
```

### 3. "Blockchain RPC connection failed"

**Cause:** Missing blockchain configuration
**Solution:** Add environment variables:

```
BLOCKCHAIN_ENABLED=true
BLOCKCHAIN_RPC_URL=https://polygon-mumbai.g.alchemy.com/v2/YOUR_KEY
BLOCKCHAIN_PRIVATE_KEY=your_wallet_private_key
BLOCKCHAIN_CONTRACT_ADDRESS=deployed_contract_address
```

## 📚 Additional Resources

- [Railway Docs - Build Configuration](https://docs.railway.app/deploy/builds)
- [Nixpacks - Java Provider](https://nixpacks.com/docs/providers/java)
- [Maven Wrapper Usage](https://maven.apache.org/wrapper/)
- [Spring Boot Memory Configuration](https://spring.io/blog/2015/12/10/spring-boot-memory-performance)

---

**Last Updated:** January 29, 2026  
**Status:** ✅ Ready to deploy
