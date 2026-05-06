#!/usr/bin/env bash
set -euo pipefail

model_repo="${LLAMA_SERVER_HF_REPO:-ggml-org/gpt-oss-20b-GGUF}"
host="${LLAMA_SERVER_HOST:-127.0.0.1}"
port="${LLAMA_SERVER_PORT:-8080}"
ctx_size="${LLAMA_SERVER_CTX_SIZE:-0}"
reasoning="${LLAMA_SERVER_REASONING:-off}"
offline="${LLAMA_SERVER_OFFLINE:-true}"

args=(
    -hf "$model_repo"
    --ctx-size "$ctx_size" \
    --jinja \
    --reasoning "$reasoning" \
    --host "$host" \
    --port "$port"
)

if [ "$offline" = "true" ]; then
    args=(--offline "${args[@]}")
fi

exec llama-server "${args[@]}"
