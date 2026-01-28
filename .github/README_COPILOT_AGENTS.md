# How to Use LegalPay Copilot Agent Modes

This workspace has **4 specialized agent modes** configured to help you build, review, and strategize the LegalPay platform.

---

## 🎯 Available Agent Modes

| Agent                | Role                   | When to Use                                                   |
| -------------------- | ---------------------- | ------------------------------------------------------------- |
| **@senior-engineer** | Technical Architecture | Building features, API design, database schema, security      |
| **@senior-pm**       | Product Strategy       | Feature prioritization, roadmap, user research, metrics       |
| **@legal-advisor**   | Legal Compliance       | RBI/NPCI compliance, contract enforceability, risk assessment |
| **@finance-advisor** | Financial Modeling     | Pricing, unit economics, fundraising, revenue projections     |

---

## 🚀 How to Activate

### Method 1: Use Chat Participants (Recommended)

In GitHub Copilot Chat, start your message with the agent tag:

```
@senior-engineer How should I design the retry logic for failed eNACH transactions?
```

```
@legal-advisor Is storing payment data in AWS Mumbai region sufficient for RBI compliance?
```

### Method 2: Reference in Conversation

Simply mention the agent in your question:

```
I need the senior PM's view on whether we should support EMI in MVP.
```

### Method 3: Multi-Agent Consultation

You can consult multiple agents in sequence:

```
@senior-pm Should we add a "Dispute Resolution" feature in MVP?
[Wait for PM response]

@legal-advisor What are the legal implications of offering in-app arbitration?
```

---

## 📋 Agent Instruction Files

Each agent has a detailed instruction file in `.github/agents/`:

- `senior-engineer.md` - Technical implementation guidelines
- `senior-pm.md` - Product strategy framework
- `legal-advisor.md` - Indian legal compliance checklist
- `finance-advisor.md` - Financial modeling templates

These files configure how Copilot responds when acting as that agent.

---

## 💡 Best Practices

### 1. **Be Specific About Context**

```
❌ Bad: "How do I handle payments?"
✅ Good: "@senior-engineer How should I implement idempotent payment retries for eNACH using Razorpay's webhook system?"
```

### 2. **Use the Right Agent for the Job**

- **Technical implementation** → @senior-engineer
- **Feature priority** → @senior-pm
- **Legal risk** → @legal-advisor
- **Pricing decision** → @finance-advisor

### 3. **Reference Existing Docs**

Agents are aware of the docs folder. You can say:

```
@senior-pm Review the current roadmap in docs/PRD_PaymentAutomation.md and suggest what to cut for MVP.
```

### 4. **Ask for Cross-Functional Review**

```
I've drafted a new pricing tier. Can the @finance-advisor review margins and @senior-pm review market positioning?
```

---

## 🔄 Workflow Examples

### Example 1: Adding a New Feature

```
1. @senior-pm Is "UPI Intent" payment worth adding in MVP?
2. @senior-engineer If yes, what's the technical effort and which API should we use?
3. @finance-advisor How does this impact our transaction margins?
```

### Example 2: Regulatory Compliance Check

```
1. @legal-advisor Does our current mandate flow comply with RBI's 24-hour pre-debit rule?
2. @senior-engineer Show me the code where we send pre-debit notifications.
3. @legal-advisor Review this implementation for compliance gaps.
```

### Example 3: Pricing Strategy

```
1. @finance-advisor What should our pricing be to achieve 70% gross margin?
2. @senior-pm How does this compare to competitors like Razorpay Subscriptions?
3. @finance-advisor Model the revenue impact if we reduce setup fee to ₹50.
```

---

## 📚 Related Documents

- **Product Requirements**: `docs/PRD_PaymentAutomation.md`
- **Marketing Strategy**: `docs/Marketing_and_Expansion_Strategy.md`
- **General Copilot Instructions**: `.github/copilot-instructions.md`

---

## 🛠️ Customization

To modify an agent's behavior:

1. Edit the relevant file in `.github/agents/`
2. Reload VS Code window (`Cmd+Shift+P` → "Reload Window")
3. Start a new Copilot chat to pick up changes

---

**Pro Tip**: These agents are context-aware of the entire LegalPay project structure. They'll reference your existing docs, suggest updates, and maintain consistency across all recommendations.
