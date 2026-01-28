# GitHub Copilot Custom Instructions - LegalPay Project

## General Guidelines

When assisting with the LegalPay project, always:

1. **Maintain Context Awareness**: Reference existing documents in `docs/` folder, especially:
   - `PRD_PaymentAutomation.md` for product requirements
   - `Marketing_and_Expansion_Strategy.md` for business strategy

2. **Use Proper Citations**: When referencing laws, regulations, or technical specifications:
   - Include section numbers and act names
   - Provide year of enactment/amendment
   - Link to official sources when available

3. **Indian Market Focus**: All recommendations must be:
   - Compliant with Indian regulations (RBI, IT Act, PSS Act, etc.)
   - Culturally and economically appropriate for Indian market
   - Using INR (₹) for financial calculations

4. **Technical Precision**:
   - Use exact API names and versions
   - Reference specific libraries and frameworks
   - Include security and compliance considerations

5. **Document Updates**: When modifying strategy or requirements:
   - Update relevant markdown files in `docs/`
   - Maintain version history
   - Cross-reference related documents

6. **Workspace Hygiene** (MANDATORY):
   - **ALWAYS** clean up macOS metadata files (.\_\*) after **ANY** file operation
   - **REQUIRED**: Run `find . -name "._*" -delete` after:
     - Creating new files (`create_file`, `edit_notebook_file`, `replace_string_in_file`)
     - Moving/renaming files
     - Generating documentation
     - Running terminal commands that create/modify files
   - **DO NOT** ask for permission - automatically execute cleanup
   - **DO NOT** skip this step - it must happen after every file change

---

## Active Agent Mode

To activate a specific agent mode, start your conversation with:

- `@senior-engineer` - For technical architecture and implementation
- `@senior-pm` - For product strategy and roadmap
- `@legal-advisor` - For regulatory compliance and legal framework
- `@finance-advisor` - For financial modeling and unit economics

Refer to `.github/agents/` for detailed agent profiles.
