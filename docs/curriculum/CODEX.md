# CODEX.md — Read this first

You are collaborating with ChatGPT (creative curriculum/editorial lead) on Deep Flow Language's original English curriculum. Read PROJECT_BRIEF.md, STORY_BIBLE.md, TECHNICAL_SPEC.md, and curriculum_manifest.json before touching code.

## Immediate task
1. Inspect the current repository, branches, stack, existing learning/content models, tests, TTS providers, usage-limit mechanisms and CI. Do not guess.
2. Propose a minimal safe integration architecture compatible with the existing app.
3. Add a JSON schema and validator for curriculum_manifest.json plus unit tests. Verify 12 core + 60 expansions, unique IDs, required sections, alignment and duration metadata.
4. Create a placeholder pipeline for C01 and Z01 only (not fabricated finished lesson text). Show how actual scripts, vocabulary assignments, Q&A and audio will flow.
5. Report findings, changed files, commands, tests, risks and next steps. Do not deploy or trigger paid API calls without approval.

## Division of responsibilities
- ChatGPT / human editorial: narrative arcs, character voice, scripts, language quality, vocabulary pedagogy and editorial approval.
- Codex: schemas, parsers, vocabulary accounting, scheduling, content QA automation, TTS integration, caching/cost caps, playback integration, tests, GitHub changes once authorized.

## Nonnegotiables
- 100% original expressive content; methods may be inspired by existing teaching and storytelling frameworks.
- Core 30–45 minutes TOTAL, expansions ~15 minutes TOTAL; audio sections playable separately.
- Word Discovery explains selected hard-to-retain words, NOT all 250.
- 3,000 words is an UNVERIFIED target until canonical licensed source and coverage audit exist.
- Vietnamese zodiac uses Cat (Mão), not Rabbit.
- Expansions optional; never block core progress.
- Never put API keys in code or git; build cost/rate-limit guardrails.
- Never silently change approved course scope or overwrite existing production content.
