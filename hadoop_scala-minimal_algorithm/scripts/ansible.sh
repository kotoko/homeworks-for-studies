#!/bin/bash
set -e

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
cd "$DIR"

# virtualenv z ansible
# $DIR to ścieżka do katalogu w którym znajduje się ten skrypt
source "$DIR/../venv3.7/bin/activate"

ansible-playbook -i ../ansible/inventory ../ansible/hadoop.yaml -t "$@"
