# AGENTS.md

This file defines how Claude AI agents coordinate to implement v2.0 features for byte-to-object-converter using the Spec-Based Development (SDD) methodology.

## Project Overview

**byte-to-object-converter v2.0** adds bidirectional conversion (Object ↔ byte[]) while removing Spring Framework dependency. See:
- `spec/v2-bidirectional-conversion/requirements.md` — What needs to be built
- `spec/v2-bidirectional-conversion/design.md` — How the architecture works
- `spec/v2-bidirectional-conversion/tasks.md` — Execution order and dependencies

## Multi-Agent Development Strategy

This project divides v2.0 implementation into **3 parallel agent teams**, each owning a distinct phase:

### Phase 1: Core Implementation (Parallel, ~2-3 hours total)
Both agents work in parallel; no blocking dependencies.

**Agent 1: Bug Fixer & Build Config**
- **Task T-1**: Fix validateArguments() error message in DeconversionHelper.java (15 min)
  - Change "targetObject must be null" → "targetObject must not be null"
  - Change ConvertFailException → NullInputException
- **Task T-3**: Update pom.xml version from 1.1.4 → 2.0.0 (10 min)
- **Blocking on**: Nothing (can start immediately)
- **Unblocks**: Agent 3
- **Verification**: `mvn clean install -DskipTests` succeeds

**Agent 2: Exception Architecture**
- **Task T-2**: Implement exception class hierarchy (2 hours)
  - Create 12 exception classes: ConvertFailException (base), ValidationException, NullInputException, InvalidAnnotationException, MissingFormatException, NegativeLengthException, TypeConversionException, DateParsingException, NumberParsingException, ReflectionException, FieldAccessException, ConstructorInvocationException
  - All extend RuntimeException chain
  - Support both `(String message)` and `(String message, Throwable cause)` constructors
  - Standardize message format: "[ClassName]: [message]"
- **Blocking on**: Nothing (can start immediately)
- **Unblocks**: Agent 3, Agent 4
- **Verification**: `mvn compile` succeeds; exception hierarchy is correct via instanceof checks

### Phase 2: Testing & Validation (Parallel, ~3-4 hours total)
Both agents start after Agent 2 completes exception classes.

**Agent 3: Test Engineer**
- **Task T-4**: Write comprehensive test suite (3 hours)
  - T-4.1: Test DataAlignment.RIGHT padding (left-align with right padding)
  - T-4.2: Test @Ignorable with null field (field skipped, no bytes written)
  - T-4.3: Test @Ignorable with non-null field (normal serialization)
  - T-4.4: Test null targetObject throws NullInputException
  - T-4.5: Test convertInputStream() direct method usage
  - Add tests to: `src/test/java/io/github/libedi/converter/ByteToObjectConverterTest.java`
- **Blocking on**: Agent 2 (exception classes must exist first)
- **Unblocks**: Agent 4, Agent 5
- **Verification**: `mvn test` passes all tests (existing + 5 new)

**Agent 4: Documentation Writer (README)**
- **Task T-5**: Update README.md and README_kr.md (1.5 hours)
  - Add deconvert() usage section with examples
  - Explain DataAlignment.LEFT vs .RIGHT with visual examples
  - Document @Ignorable annotation (when/why to use)
  - Add Spring Framework removal notice
  - Include exception handling guide with code examples
  - Verify all code snippets are syntactically correct and match library API
- **Blocking on**: Agent 2 (need exception classes to document exception handling)
- **Unblocks**: Nothing
- **Verification**: Code examples compile; documentation is clear and consistent

### Phase 3: Final Documentation (Sequential, ~1 hour)
Starts after Agent 3 completes testing.

**Agent 5: Documentation Writer (CLAUDE.md)**
- **Task T-6**: Update CLAUDE.md for v2.0 (1 hour)
  - Update "Architecture" section: explain Helper separation and exception hierarchy
  - Update "Dependencies" section: mark Spring as removed, add commons-lang3 3.14.0
  - Add new "Common Tasks" subsection: bidirectional conversion usage
  - Update "Exception Handling" section with new hierarchy
  - Add "References" to spec/ documents
  - Cross-reference changes with README updates
- **Blocking on**: Agent 3 (need test results to document testing patterns), Agent 4 (need README updates for consistency)
- **Unblocks**: Release readiness check
- **Verification**: CLAUDE.md is consistent with README.md and actual code

## Execution Timeline

```
Phase 1 (Day 1-2):
  Agent 1: T-1 (15 min) + T-3 (10 min)
  Agent 2: T-2 (2 hours)
           ↓ (unblock)

Phase 2 (Day 2-3):
  Agent 3: T-4 (3 hours)
           ↓ (unblock)
  Agent 4: T-5 (1.5 hours)  [can start after Agent 2, or overlap]

Phase 3 (Day 3):
  Agent 5: T-6 (1 hour)

Total: ~8 hours actual work across 2-3 calendar days
```

## Inter-Agent Communication

### Before Starting
- All agents **must read** `spec/v2-bidirectional-conversion/` documents first
- Agent 2 (Exception Architecture) **owns** the exception API — document exact class names and constructors
- Agent 3 (Test Engineer) **consults** Agent 2 for correct exception class names before writing tests
- Agent 4 (README) **consults** Agent 3 for test patterns and Agent 2 for exception details

### During Execution
- **Blockers**: If an agent is blocked, notify the team (e.g., "waiting for Agent 2 exception classes")
- **API changes**: If exception names change, Agent 2 **must notify Agent 3 immediately**
- **Example code**: If Agent 4 finds a code snippet doesn't work, **escalate to the code owner**

### After Completion
- Each agent creates a **separate commit** per task (T-1, T-2, T-3, etc.)
- Commit message format: Follow `tasks.md` Section 9 (Changelog) exactly
- Push to branch: `11-project-version-2`
- No rebasing/force-push; cherry-pick only if conflicts arise

## Definition of Done (Per Task)

### T-1 Complete
- [ ] DeconversionHelper.validateArguments() fixed
- [ ] Error message: "targetObject must not be null"
- [ ] Exception type: NullInputException
- [ ] `mvn test` passes existing tests

### T-2 Complete
- [ ] All 12 exception classes exist in `src/main/java/io/github/libedi/converter/exception/`
- [ ] Hierarchy verified: extends chain is correct
- [ ] Message format "[ClassName]: message" enforced
- [ ] Both constructors (message, message+cause) implemented
- [ ] `mvn compile` succeeds

### T-3 Complete
- [ ] pom.xml version: 2.0.0
- [ ] `mvn verify` succeeds (POM is valid)

### T-4 Complete
- [ ] 5 new test methods added to ByteToObjectConverterTest.java
- [ ] All tests pass: `mvn test -Dtest=ByteToObjectConverterTest`
- [ ] Test coverage: DataAlignment.RIGHT, @Ignorable null/non-null, null input, convertInputStream()

### T-5 Complete
- [ ] README.md contains 5 new sections (deconvert, DataAlignment, @Ignorable, Spring removal, exception handling)
- [ ] README_kr.md updated with same content (if exists)
- [ ] All code examples compile and match API
- [ ] No dead links or outdated references

### T-6 Complete
- [ ] CLAUDE.md updated in all sections: Architecture, Dependencies, Common Tasks, Exception Handling, References
- [ ] Consistent with README.md and actual code
- [ ] No broken internal cross-references

## Verification Checklist (Final)

After all agents complete, **one agent verifies**:

- [ ] `git status` shows only intended files modified
- [ ] `mvn clean install` succeeds
- [ ] `mvn test` passes all tests (including 5 new ones)
- [ ] `mvn javadoc:javadoc` generates docs without warnings
- [ ] README.md code snippets execute without error
- [ ] CLAUDE.md matches actual code behavior
- [ ] All 6 commits follow message format in `tasks.md` Section 9
- [ ] No unintended changes to pom.xml, .gitignore, or non-source files

## Success Criteria

v2.0 is **ready to merge** when:

1. ✅ All Phase 1 + Phase 2 + Phase 3 tasks complete
2. ✅ All verification checks pass
3. ✅ User approval: "승인!" in conversation
4. ✅ PR review: `/code-review` passes (or manual review approves)
5. ✅ Merge to main + tag v2.0.0 + publish to Maven Central (separate process)

## Notes for Future Agents

- This is a **Spec-Based Development (SDD)** project: always read the spec documents first
- **No architectural changes without spec approval** — if code disagrees with spec, fix the code, not the spec
- **Javadoc must be complete** — protected methods, not just public
- **Test-first mindset**: T-4 tests should cover edge cases (null inputs, boundary conditions, enum values)
- **Documentation for end-users**: README examples should work out-of-the-box
- **CLAUDE.md is for future Claude agents**: write it as if explaining to another AI developer
