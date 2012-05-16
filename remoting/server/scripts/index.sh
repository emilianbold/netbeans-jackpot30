DIR=`dirname $0`
USERDIR=`mktemp -d`;
trap "rm -rf -- '$USERDIR'" EXIT

ID="$1"; shift
NAME="$1"; shift
TARGET="$1"; shift
ROOT_DIR="$1"; shift

$DIR/indexer/bin/indexer --userdir $USERDIR --nosplash --nogui -J-Xmx2048m -J-Dnetbeans.indexing.recursiveListeners=false --category-id "$ID" --category-name "$NAME" --cache-target "$TARGET" --category-root-dir "$ROOT_DIR" --category-projects "$@"

exit
