#!/usr/bin/env bash
set -euo pipefail

script_dir=$(dirname "$0")

export AGENT_APPLICATION="${script_dir}/.."
export SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-llama-server}"
export MAVEN_PROFILES="${MAVEN_PROFILES:-llama-server-models}"

"$script_dir/support/agent.sh"
