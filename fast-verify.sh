#!/usr/bin/env bash
# 跳过 res 复制。增量编译：只重编比 .class 新的 .java。
# Nashorn import 只在 UtilText 内容变化时改写，编完立刻 restore，并把源文件 mtime 对齐 .class，避免下次全量/重复编 UtilText。
# EXIT trap 保证成功/失败/中断都会 restore-imports。发版用 mvn clean package / build.ps1。
set -euo pipefail
cd "$(dirname "$0")"

UTIL_SRC="src/com/lilithsthrone/game/dialogue/utils/UtilText.java"
UTIL_CLASS="target/classes/com/lilithsthrone/game/dialogue/utils/UtilText.class"
STAMP="target/fast-verify-utiltext.stamp"

patched=0
restore_imports() {
	if [[ "$patched" -eq 1 ]]; then
		mvn -o -q antrun:run@restore-imports || true
		if [[ -f "$UTIL_CLASS" && -f "$UTIL_SRC" ]]; then
			touch -r "$UTIL_CLASS" "$UTIL_SRC"
		fi
	fi
}
trap restore_imports EXIT

src_hash() {
	md5sum "$UTIL_SRC" | awk '{print $1}'
}

need_patch=1
if [[ -f "$STAMP" && -f "$UTIL_CLASS" && -f "$UTIL_SRC" ]]; then
	if [[ "$(cat "$STAMP")" == "$(src_hash)" ]]; then
		need_patch=0
	fi
fi

if [[ "$need_patch" -eq 1 ]]; then
	mvn -o -q antrun:run@correct-imports
	patched=1
fi

if [[ ! -f target/classes/com/lilithsthrone/main/Main.class ]]; then
	echo "target/classes 不完整，先做一次跳过资源的全量 compile..."
	mvn -o -q -Dmaven.resources.skip=true compiler:compile
fi

mvn -o -q \
	-Dmaven.resources.skip=true \
	compiler:compile compiler:testCompile surefire:test

if [[ "$patched" -eq 1 ]]; then
	restore_imports
	patched=0
	mkdir -p target
	src_hash > "$STAMP"
fi
