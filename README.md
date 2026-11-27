# comp1337

comp1337 ("comp elite") provides locally deployed LLM code completions for PyCharm:
ghost-text inline suggestions served by a fill-in-the-middle (FIM) model on your own
machine. Mayday — from the French *venez m'aider*, "come help me" — for when cloud
Copilot is too slow. Nothing leaves your machine; all traffic is loopback-only.

## How it works

- Implements the IntelliJ Platform inline completion extension point
  (`com.intellij.inline.completion.provider`, platform 2024.2+), so suggestions render
  as ghost text and accept with Tab.
- On each trigger it sends the text around the caret (windowed by `prefixChars` /
  `suffixChars`) to a local HTTP server:
  - **LM Studio / Ollama** (`backend = openai`): `POST {endpoint}/completions` with a
    `<|fim_prefix|>…<|fim_suffix|>…<|fim_middle|>` prompt and FIM stop tokens.
  - **llama.cpp `llama-server`** (`backend = llamacpp`): `POST {endpoint}/infill` with
    `input_prefix` / `input_suffix` and `cache_prompt` for KV-cache reuse.
- Every request is wrapped in `withTimeoutOrNull` and is coroutine-cancellable; the
  platform cancels stale requests as you keep typing, and a startup activity warms the
  model so the first completion does not pay load time.

## Recommended setup (RTX 5060, 8 GB VRAM)

Qwen2.5-Coder-1.5B, Q4_K_M GGUF (base variant for true FIM) in **LM Studio ≥ 0.3.15**
(the CUDA 12.8 engine auto-selected for RTX 50-series needs a current 580+ driver).
~1 GB VRAM, leaving room for the KV cache; 64 tokens land well under a second on GPU.

1. In LM Studio: download the model, Load it with GPU offload at 100% and context 4096.
2. `lms server start` — binds `127.0.0.1:1234` by default; keep "Serve on Local Network"
   off. JIT-loaded models stay resident (no idle unload), or preload with `lms load`.
3. `curl http://127.0.0.1:1234/v1/models` and copy the exact model id.
4. In PyCharm: Settings → Tools → comp1337 — set endpoint `http://127.0.0.1:1234/v1`,
   backend `openai`, model id from step 3, max tokens 64, timeout 1500 ms.

Smoke-test the FIM path without the IDE:

```bash
curl http://127.0.0.1:1234/v1/completions -H 'Content-Type: application/json' -d '{
  "model": "<id-from-/v1/models>",
  "prompt": "<|fim_prefix|>def add(a, b):\n    return <|fim_suffix|>\n<|fim_middle|>",
  "max_tokens": 16, "temperature": 0, "stream": false,
  "stop": ["<|fim_suffix|>", "<|fim_middle|>"]
}'
```

## Build and run

Requires JDK 17+. Targets PyCharm 2024.2+ (`sinceBuild = 242`).

```bash
gradle wrapper          # once, to generate ./gradlew (needs network access)
./gradlew runIde        # sandboxed PyCharm with the plugin installed
./gradlew buildPlugin   # plugin zip in build/distributions
```

Install the zip via Settings → Plugins → gear → Install Plugin from Disk.

## Settings

| Setting | Default | Meaning |
| --- | --- | --- |
| Enabled | true | master switch |
| Backend | `openai` | `openai` (`/v1/completions`) or `llamacpp` (`/infill`) |
| Endpoint | `http://127.0.0.1:1234/v1` | loopback base URL of the model server |
| Model | `qwen2.5-coder-1.5b` | exact id from `/v1/models` |
| Max tokens | 64 | generation budget per suggestion |
| Prefix / suffix chars | 4000 / 1000 | prompt window around the caret |
| Timeout (ms) | 1500 | hard cap per request; slow responses are dropped |
