# TECHNICAL SPECIFICATION — Codex implementation handoff

## Objective
Add a reproducible, reviewable **content pipeline** for the 72-lesson curriculum into the existing Deep Flow Language project. **Inspect the repository first**; do not assume tech stack, architecture, API keys, deployment permissions, or audio vendor. Avoid breaking current app. Start with tests and a minimal vertical slice C01 + Z01 using placeholder content; never fabricate finished scripts or word coverage.

## Recommended data model (adapt to existing stack)
- `lesson`: id, type, title, linked_core, status, language, CEFR target, source_provenance, content_version.
- `section`: lesson_id, section_type, script_segments, target_duration, measured_duration, audio_asset_id.
- `segment`: stable id, speaker, text, pause_ms, optional text alignment and translation.
- `vocabulary`: stable id, lemma, part_of_speech, canonical source and license, frequency band, difficulty factors, pronunciation, optional gloss.
- `lesson_vocabulary`: lesson_id, vocab_id, role (`new_target` / `review` / `incidental`), occurrences by section.
- `question`: section_id, prompt, expected_answer, alternate_acceptable_answers, answer_pause_ms, referenced story segment, target vocabulary.
- `review_event`: learner_id, vocab_id, timestamp, mode (listening recognition / comprehension / active use), correctness, latency and scheduler state.
- `audio_asset`: section_id, TTS provider, voice, model/version, generation timestamp, duration_ms, cost estimate, storage path, license/usage flags.

## Processing pipeline
1. Import/validate licensed 3,000-entry canonical vocabulary source. Never claim target met before importing and validating. Decide tokenization/lemmatization and handling of phrasal verbs and homographs.
2. Plan word assignments per core lesson (target 250 unique new entries) and spaced reappearances. Validate coverage and duplicate new assignments; avoid mandatory expansion dependency.
3. Draft original scripts in structured segments with provenance and revision history. Separate text generation from quality approval.
4. Generate Q&A with pauses and expected answers; verify each question is supported by the story.
5. TTS provider adapter using environment secrets, never embed credentials. Rate limits, retry/backoff, concurrency caps, token/cost budgets, cache by content hash, idempotent jobs, failure resume, explicit user-controlled spending limits.
6. Measure **actual** audio duration with pauses. Flag core <30 or >45 only as editorial warning if intentional; hard-fail >45 if strict production policy chosen. Expansion target ~15 with tolerance to define.
7. Export player-ready assets and text/transcript alignment for listen, read+listen, read-only modes. Support per-section playback and resume.
8. Review dashboard: target words assigned, missing words, repeat distribution, content status, audio status, estimated costs, review queue.

## MVP acceptance tests
- Manifest parses and has exactly 12 core and 60 expansions, unique ids, valid C01–C12 / Z01–Z60 mapping, each expansion aligned to exactly one core lesson.
- Core section ids exactly `main_story`, `word_discovery`, `quick_response`, `time_shift`; expansion section ids `daily_adventure`, `quick_response`.
- 3,000-word validator reports **not ready** if canonical list absent; no fabricated pass.
- 250 new targets per core only after actual word assignment, and each target assigned once as new; cross-lesson review allowed.
- Q&A schema requires prompt, expected answer and nonnegative pause.
- TTS mock can generate a complete C01/Z01 pipeline without spending API money.
- Secrets are not logged or committed; no automatic production deployment or spending.
- CI test suite and clear CLI commands to validate and build content.

## Delivery sequence
Phase A: inspect repo and report existing architecture and safe integration plan; no code changes until scope understood.
Phase B: add manifest schema, validation tests and content structure; import manifest.
Phase C: add vocabulary import, coverage audit and review scheduler.
Phase D: add authoring/Q&A QA and TTS mock then real adapter behind explicit configuration.
Phase E: integrate app playback and progress tracking, gated rollout.

## Important constraints
All current story outlines are editorial proposals. Do not claim any lesson has been authored, voiced, verified, or shipped. Ask for approval before paid API operations, irreversible changes or deployment. Do not create a new application when integration into the existing app is feasible.
