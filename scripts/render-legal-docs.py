#!/usr/bin/env python3
"""Собирает HTML-страницы правовых документов из markdown-исходников.

Исходники и результат лежат рядом в src/main/resources/legal:
    privacy-policy.md  ->  privacy-policy.html

HTML открывается вебвью мобильного приложения, поэтому страница самодостаточна:
никаких внешних шрифтов, скриптов и картинок — только инлайновый CSS.

Запуск после правки любого .md:
    python3 scripts/render-legal-docs.py
"""

import html
import re
import sys
from pathlib import Path

LEGAL_DIR = Path(__file__).resolve().parent.parent / "src/main/resources/legal"

TEMPLATE = """<!DOCTYPE html>
<html lang="ru">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover">
<meta name="robots" content="noindex">
<title>{title}</title>
<style>
:root {{
  color-scheme: light dark;
  --bg: #ffffff;
  --surface: #f6f6f7;
  --text: #16161a;
  --muted: #6b6b76;
  --line: #e3e3e8;
  --accent: #d98324;
}}
@media (prefers-color-scheme: dark) {{
  :root {{
    --bg: #121214;
    --surface: #1c1c20;
    --text: #ececf1;
    --muted: #9a9aa5;
    --line: #2c2c33;
    --accent: #f0a95a;
  }}
}}
* {{ box-sizing: border-box; }}
html {{ -webkit-text-size-adjust: 100%; }}
body {{
  margin: 0;
  padding: 24px 20px calc(48px + env(safe-area-inset-bottom));
  background: var(--bg);
  color: var(--text);
  font: 16px/1.6 -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
  overflow-wrap: break-word;
}}
main {{ max-width: 720px; margin: 0 auto; }}
h1 {{ font-size: 24px; line-height: 1.25; margin: 0 0 4px; letter-spacing: -0.01em; }}
.subtitle {{ font-size: 17px; color: var(--muted); margin: 0 0 12px; font-weight: 500; }}
.meta {{ font-size: 14px; color: var(--muted); margin: 0 0 28px; }}
h2 {{
  font-size: 18px;
  line-height: 1.3;
  margin: 0 0 12px;
}}
section {{
  margin-top: 32px;
  padding-top: 24px;
  border-top: 1px solid var(--line);
}}
p {{ margin: 0 0 12px; }}
ul {{ margin: 0 0 12px; padding-left: 22px; }}
li {{ margin-bottom: 6px; }}
strong {{ font-weight: 600; }}
a {{ color: var(--accent); }}
nav {{
  background: var(--surface);
  border-radius: 12px;
  padding: 16px 18px;
}}
nav .nav-title {{
  font-size: 13px;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  color: var(--muted);
  margin: 0 0 10px;
}}
nav ol {{ margin: 0; padding-left: 20px; font-size: 15px; }}
nav li {{ margin-bottom: 6px; }}
nav a {{ color: var(--text); text-decoration: none; }}
.table-wrap {{ overflow-x: auto; -webkit-overflow-scrolling: touch; margin: 0 0 16px; }}
table {{ border-collapse: collapse; width: 100%; min-width: 460px; font-size: 14px; }}
th, td {{ border: 1px solid var(--line); padding: 8px 10px; text-align: left; vertical-align: top; }}
th {{ background: var(--surface); font-weight: 600; }}
/* На узком экране трёхколоночные таблицы не читаются:
   разворачиваем каждую строку в карточку с подписями из шапки. */
@media (max-width: 560px) {{
  .table-wrap {{ overflow-x: visible; }}
  table, tbody, tr, td {{ display: block; width: 100%; }}
  table {{ min-width: 0; }}
  thead {{ display: none; }}
  tr {{
    border: 1px solid var(--line);
    border-radius: 10px;
    margin-bottom: 12px;
    padding: 4px 0;
  }}
  td {{ border: none; padding: 8px 12px; }}
  td::before {{
    content: attr(data-label);
    display: block;
    font-size: 12px;
    text-transform: uppercase;
    letter-spacing: 0.05em;
    color: var(--muted);
    margin-bottom: 2px;
  }}
}}
</style>
</head>
<body>
<main>
{body}
</main>
</body>
</html>
"""

BOLD = re.compile(r"\*\*(.+?)\*\*")
ITALIC = re.compile(r"(?<!\*)\*(?!\s)(.+?)(?<!\s)\*(?!\*)")
SECTION_NUMBER = re.compile(r"^(\d+)\.")
LEADING_NUMBER = re.compile(r"^\d+\.\s*")


def inline(text: str) -> str:
    text = html.escape(text, quote=False)
    text = BOLD.sub(r"<strong>\1</strong>", text)
    return ITALIC.sub(r"<em>\1</em>", text)


def anchor_of(heading: str) -> str:
    number = SECTION_NUMBER.match(heading)
    return f"section-{number.group(1)}" if number else "section"


def render(md: str) -> str:
    lines = md.split("\n")
    out: list[str] = []
    toc: list[tuple[str, str]] = []
    paragraph: list[str] = []
    bullets: list[str] = []
    state = {"section_open": False, "meta_done": False}
    i = 0

    def flush_paragraph() -> None:
        if not paragraph:
            return
        # первый абзац после подзаголовка — строка «Редакция от …», она идёт мелким шрифтом
        head_block = not state["section_open"] and not state["meta_done"] and bool(out)
        css = ' class="meta"' if head_block else ""
        if head_block:
            state["meta_done"] = True
        out.append(f"<p{css}>" + "<br>".join(inline(x) for x in paragraph) + "</p>")
        paragraph.clear()

    def flush_bullets() -> None:
        if not bullets:
            return
        out.append("<ul>")
        out.extend(f"<li>{inline(x)}</li>" for x in bullets)
        out.append("</ul>")
        bullets.clear()

    def flush() -> None:
        flush_paragraph()
        flush_bullets()

    while i < len(lines):
        line = lines[i].rstrip()
        stripped = line.strip()

        if not stripped or stripped == "---":
            flush()
        elif line.startswith("# "):
            flush()
            out.append(f"<h1>{inline(stripped[2:].strip())}</h1>")
        elif line.startswith("## ") and out and out[-1].startswith("<h1>"):
            out.append(f'<p class="subtitle">{inline(stripped[3:].strip())}</p>')
        elif line.startswith("## "):
            flush()
            if state["section_open"]:
                out.append("</section>")
            heading = stripped[3:].strip()
            anchor = anchor_of(heading)
            toc.append((anchor, heading))
            out.append(f'<section id="{anchor}"><h2>{inline(heading)}</h2>')
            state["section_open"] = True
        elif line.startswith("|"):
            flush()
            rows = []
            while i < len(lines) and lines[i].strip().startswith("|"):
                rows.append([c.strip() for c in lines[i].strip().strip("|").split("|")])
                i += 1
            i -= 1
            out.append('<div class="table-wrap"><table><thead><tr>')
            out.extend(f"<th>{inline(c)}</th>" for c in rows[0])
            out.append("</tr></thead><tbody>")
            headers = rows[0]
            for row in rows[2:]:
                cells = (
                    f'<td data-label="{html.escape(headers[n]) if n < len(headers) else ""}">{inline(c)}</td>'
                    for n, c in enumerate(row)
                )
                out.append("<tr>" + "".join(cells) + "</tr>")
            out.append("</tbody></table></div>")
        elif line.startswith("- "):
            flush_paragraph()
            bullets.append(stripped[2:].strip())
        else:
            flush_bullets()
            paragraph.append(stripped)
        i += 1

    flush()
    if state["section_open"]:
        out.append("</section>")

    nav = ['<nav><p class="nav-title">Содержание</p><ol>']
    nav += [f'<li><a href="#{a}">{inline(LEADING_NUMBER.sub("", h))}</a></li>' for a, h in toc]
    nav.append("</ol></nav>")

    # оглавление ставим после шапки документа: заголовок, подзаголовок, редакция
    head_end = next((n for n, chunk in enumerate(out) if chunk.startswith("<section")), len(out))
    out[head_end:head_end] = nav
    return "\n".join(out)


def main() -> int:
    sources = sorted(LEGAL_DIR.glob("*.md"))
    if not sources:
        print(f"нет .md в {LEGAL_DIR}", file=sys.stderr)
        return 1
    for src in sources:
        md = src.read_text(encoding="utf-8")
        title = md.split("\n", 1)[0].lstrip("# ").strip()
        dst = src.with_suffix(".html")
        dst.write_text(TEMPLATE.format(title=html.escape(title), body=render(md)), encoding="utf-8")
        print(f"{src.name} -> {dst.name} ({dst.stat().st_size} байт)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
