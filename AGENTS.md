# Repository Guidelines

## Project Structure & Module Organization

This repository is in the planning stage for a coffee tasting content Agent. Keep documents organized under `docs/`:

- `docs/prd/`: product requirements and scope.
- `docs/architecture/`: Agent architecture, module design, memory design, and service boundaries.
- `docs/research/`: platform, API, flavor, and feasibility research.
- `docs/meeting-notes/`: discussion notes and requirement alignment records.

When source code is introduced, prefer conventional Java backend layout:

- `src/main/java/`: application source code.
- `src/main/resources/`: configuration, prompts, and templates.
- `src/test/java/`: unit and integration tests.
- `assets/`: sample images, screenshots, or non-code artifacts when needed.

Do not place project documents in the repository root unless they are repository-level guides such as `README.md` or `AGENTS.md`.

## Build, Test, and Development Commands

No build system has been added yet. Once the Java backend is initialized, document the exact commands here. Expected commands:

- `./mvnw test` or `./gradlew test`: run automated tests.
- `./mvnw spring-boot:run` or `./gradlew bootRun`: run the backend locally.
- `./mvnw verify` or `./gradlew check`: run full verification.

Avoid inventing commands before the toolchain exists.

## Coding Style & Naming Conventions

Follow existing conventions first. For Java code, use clear package boundaries around Agent capabilities such as `context`, `planning`, `memory`, `tools`, and `multiagent`.

Use descriptive names:

- Documents: lowercase kebab-case, for example `agent-architecture-v0.1.md`.
- Java classes: `PascalCase`.
- Methods and variables: `camelCase`.
- Constants: `UPPER_SNAKE_CASE`.

Keep changes focused and avoid broad refactors unless they directly support the current task.

Do not hardcode model prompts in Java, TypeScript, tests, or documentation examples that look like runtime code. Prompt text for Agent behavior, field definitions, fact-boundary rules, tool instructions, and output contracts must live under `backend/src/main/resources/prompts/` with a scenario and versioned filename, and runtime code must load the template through a prompt loader. Only stable resource paths, placeholder names, and enum values may remain in code.

## Testing Guidelines

Add tests with new behavior. Place Java tests under `src/test/java/` and name them after the unit under test, for example `MemoryRetrieverTest`.

For Agent behavior, test structured inputs, tool outputs, memory retrieval, and prompt constraints. If external APIs block full testing, document the gap and provide a local verification path.

When adding or changing a prompt template, add tests that prove the template is loaded, dynamic placeholders are replaced, and no runtime request depends on duplicated hardcoded prompt text.

## Commit & Pull Request Guidelines

This repository has no commit history yet, so no local convention exists. Use concise, imperative messages such as:

- `docs: add coffee agent PRD`
- `feat: add memory retrieval prototype`
- `test: cover flavor suggestion ranking`

Pull requests should include a short summary, verification steps, linked issues or decisions when applicable, and screenshots for UI changes. For research-backed changes, link the relevant document under `docs/research/`.

## Agent-Specific Instructions

Follow [AI_Coding_行为准则.md](./AI_Coding_行为准则.md) for project collaboration. In particular: read existing docs before changing files, ask when requirements are materially ambiguous, verify changes when possible, and clearly state any unverified assumptions.

Follow [.specify/memory/constitution.md](./.specify/memory/constitution.md) for project governance. New specs, plans, tasks, and implementation reviews must preserve truthful coffee records, trace Agent state, confirm high-impact tool actions, deliver verified vertical slices, and keep the architecture learning-oriented.

All generated project documentation must use Simplified Chinese by default, except for code identifiers, commands, API fields, library names, and required source quotations. This includes PRDs, architecture notes, research documents, spec-kit specs, plans, tasks, reviews, and learning materials.

<!-- SPECKIT START -->
For additional context about technologies to be used, project structure,
shell commands, and other important information, read the current plan
at specs/005-model-message-routing/plan.md
<!-- SPECKIT END -->
