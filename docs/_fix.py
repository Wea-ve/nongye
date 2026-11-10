# -*- coding: utf-8 -*-
import re

path = r"D:\若依\nongye\docs\build_tech_doc.py"
text = open(path, encoding="utf-8").read()
lines = text.split("\n")
fixed = []
count = 0
for ln in lines:
    m = re.match(r"^(\s*\],\s*\[[0-9.]+,?\s?[0-9.]*,\s?[0-9.]*,?\s?[0-9.]*\]\))\)$", ln)
    if m:
        fixed.append(ln + ")")
        count += 1
    else:
        fixed.append(ln)
open(path, "w", encoding="utf-8", newline="\n").write("\n".join(fixed))
print("fixed lines:", count)
