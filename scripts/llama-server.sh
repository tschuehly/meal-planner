#!/usr/bin/env bash
set -euo pipefail

model_repo="${LLAMA_SERVER_HF_REPO:-ggml-org/gemma-4-26B-A4B-it-GGUF:Q4_K_M}"
host="${LLAMA_SERVER_HOST:-127.0.0.1}"
port="${LLAMA_SERVER_PORT:-8080}"
reasoning="${LLAMA_SERVER_REASONING:-off}"

exec llama-server \
    --offline \
    -hf "$model_repo" \
    --reasoning "$reasoning" \
    --host "$host" \
    --port "$port"
