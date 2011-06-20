DIR=`dirname $0`
USERDIR=`tempfile`;
rm $USERDIR
mkdir -p $USERDIR
trap "rm -rf -- '$USERDIR'" EXIT

ID="$1"; shift
NAME="$1"; shift
TARGET="$1"; shift
ROOT_DIR="$1"; shift

$DIR/indexer/bin/backend --userdir $USERDIR --nosplash --nogui -J-Xmx2048m --category-id "$ID" --category-name "$NAME" --cache-target "$TARGET" --category-root-dir "$ROOT_DIR" --category-projects "$@"

exit
