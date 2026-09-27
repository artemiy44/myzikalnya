"""Turns Lucide / Tabler SVG icons into the ImageVector code used in ui/icons/IconVectors.kt.

Usage: python3 svg2vector.py <KotlinName> <file.svg> [lucide|tabler] [filled]
Prints a `internal val <KotlinName>: ImageVector by lazy { ... }` block to paste into
IconVectors.kt (and add a getter in AppIcons.kt). Strokes are 2px with round ends, as in the
source sets; Lucide drawings get LUCIDE_SCALE. Every icon is a 24x24 grid.
"""
import re, sys, xml.etree.ElementTree as ET

NS = "{http://www.w3.org/2000/svg}"

def num(v): return float(v) if v is not None else 0.0

def fmt(x):
    s = f"{x:.4f}".rstrip("0").rstrip(".")
    return s if s else "0"

def element_path(el):
    tag = el.tag.replace(NS, ""); a = el.attrib
    if tag == "path": return a["d"]
    if tag == "circle":
        cx, cy, r = num(a.get("cx")), num(a.get("cy")), num(a.get("r"))
        return f"M{fmt(cx-r)} {fmt(cy)}a{fmt(r)} {fmt(r)} 0 1 0 {fmt(2*r)} 0a{fmt(r)} {fmt(r)} 0 1 0 {fmt(-2*r)} 0"
    if tag == "ellipse":
        cx, cy, rx, ry = num(a.get("cx")), num(a.get("cy")), num(a.get("rx")), num(a.get("ry"))
        return f"M{fmt(cx-rx)} {fmt(cy)}a{fmt(rx)} {fmt(ry)} 0 1 0 {fmt(2*rx)} 0a{fmt(rx)} {fmt(ry)} 0 1 0 {fmt(-2*rx)} 0"
    if tag == "rect":
        x, y, w, h = num(a.get("x")), num(a.get("y")), num(a.get("width")), num(a.get("height"))
        rx = a.get("rx"); ry = a.get("ry")
        rx = num(rx) if rx is not None else (num(ry) if ry is not None else 0)
        ry = num(ry) if ry is not None else rx
        rx = min(rx, w / 2); ry = min(ry, h / 2)
        if rx == 0: return f"M{fmt(x)} {fmt(y)}h{fmt(w)}v{fmt(h)}h{fmt(-w)}z"
        return (f"M{fmt(x+rx)} {fmt(y)}h{fmt(w-2*rx)}a{fmt(rx)} {fmt(ry)} 0 0 1 {fmt(rx)} {fmt(ry)}"
                f"v{fmt(h-2*ry)}a{fmt(rx)} {fmt(ry)} 0 0 1 {fmt(-rx)} {fmt(ry)}h{fmt(-(w-2*rx))}"
                f"a{fmt(rx)} {fmt(ry)} 0 0 1 {fmt(-rx)} {fmt(-ry)}v{fmt(-(h-2*ry))}a{fmt(rx)} {fmt(ry)} 0 0 1 {fmt(rx)} {fmt(-ry)}z")
    if tag == "line":
        return f"M{fmt(num(a.get('x1')))} {fmt(num(a.get('y1')))}L{fmt(num(a.get('x2')))} {fmt(num(a.get('y2')))}"
    if tag in ("polyline", "polygon"):
        pts = re.findall(r"-?\d*\.?\d+", a["points"])
        d = "M" + " L".join(f"{pts[i]} {pts[i+1]}" for i in range(0, len(pts), 2))
        return d + ("z" if tag == "polygon" else "")
    raise ValueError(tag)

def parse(path):
    root = ET.parse(path).getroot()
    root_fill = root.get("fill", "none"); root_stroke = root.get("stroke", "none")
    out = []
    for el in root.iter():
        tag = el.tag.replace(NS, "")
        if tag in ("svg", "g", "title", "desc"): continue
        fill = el.get("fill", root_fill); stroke = el.get("stroke", root_stroke)
        if fill == "none" and stroke == "none": continue  # Tabler's invisible bounding box
        out.append((element_path(el), fill != "none", stroke != "none"))
    return out

def kt_vector(name, paths, lucide=False, force_fill=False):
    scale = ", scale = LUCIDE_SCALE" if lucide else ""
    lines = [f"internal val {name}: ImageVector by lazy {{", f'    iconVector("{name}"{scale}) {{']
    for d, fill, stroke in paths:
        lines.append(f'        path("{d}", fill = {"true" if (fill or force_fill) else "false"}, stroke = {"true" if stroke else "false"})')
    lines += ["    }", "}", ""]
    return "\n".join(lines)

if __name__ == "__main__":
    name, svg = sys.argv[1], sys.argv[2]
    kind = sys.argv[3] if len(sys.argv) > 3 else "tabler"
    filled = len(sys.argv) > 4 and sys.argv[4] == "filled"
    print(kt_vector(name, parse(svg), lucide=(kind == "lucide"), force_fill=filled))
