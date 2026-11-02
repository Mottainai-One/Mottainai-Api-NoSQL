# MongoDB setup script

`setup_full_mongosh.js` is a self-contained `mongosh` snapshot of the 24
Mottainai-IA collections, validators and indexes declared in
`scripts/mongo/schema.json` in the IA repository. It also inserts the two
code-defined fallback intents if absent. It does not copy customer records,
conversation history, memories, embeddings or cached answers.

Connect to the `mottainai` database first, then run:

```javascript
load("script/setup_full_mongosh.js")
```

The script creates missing collections, applies validators with `collMod`,
creates missing indexes and prints collection counts. It preserves existing
documents. A unique index can fail if existing documents violate its
constraint; resolve duplicates before applying that index.

At the time of comparison, the connected MongoDB had all 24 collections.
`skill_registry` and `skill_executions` lacked their current validators and
indexes; the other 22 collections matched the IA schema check.
