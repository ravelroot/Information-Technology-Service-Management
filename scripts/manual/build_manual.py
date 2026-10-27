#!/usr/bin/env python3
"""Gera o manual do Talos. em PDF (paleta minimalista do aplicativo).

Uso:
    python3 scripts/manual/build_manual.py --images <pasta-de-prints> --out "Manual-Talos-Versão 1.1.0.pdf"

Os prints são procurados primeiro em --images e depois em docs/images. Figuras sem
arquivo são omitidas sem erro, então o manual sempre pode ser gerado.
Requer: reportlab (pip install reportlab).
"""
from __future__ import annotations

import argparse
import re
from pathlib import Path

from reportlab.lib import colors
from reportlab.lib.enums import TA_LEFT
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle
from reportlab.lib.units import mm
from reportlab.lib.utils import ImageReader
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.platypus import (BaseDocTemplate, CondPageBreak, Flowable, Frame, Image, KeepTogether,
                                NextPageTemplate, PageBreak, PageTemplate, Paragraph, Spacer, Table,
                                TableStyle)
from reportlab.platypus.tableofcontents import TableOfContents

from content import VERSION, chapters  # noqa: E402  (mesmo diretório)

ROOT = Path(__file__).resolve().parents[2]
FONTS = ROOT / "talos-desktop/src/main/resources/io/talos/desktop/fonts"

# ---------------------------------------------------------------- paleta
# Paleta minimalista do Talos. (a mesma do aplicativo): cinza-claro, areia, pedra, malva e preto.
INK = colors.HexColor("#000000")
TEXT2 = colors.HexColor("#2B2727")
MUTED = colors.HexColor("#6B6262")
PAPER = colors.HexColor("#E8E8E8")
SURFACE = colors.HexColor("#FFFFFF")
SURFACE2 = colors.HexColor("#F1EFEE")
HEAD = colors.HexColor("#000000")
ZEBRA = colors.HexColor("#E8E8E8")
LINE = colors.HexColor("#D9D4D1")
LINE2 = colors.HexColor("#BAB7B5")
SAND = colors.HexColor("#D9D1CE")
STONE = colors.HexColor("#BAB7B5")
MAUVE = colors.HexColor("#AB9D9D")
DARK = colors.HexColor("#2B2727")
BLACK = colors.HexColor("#000000")
ON_DARK = colors.HexColor("#F1EFEE")
ON_DARK_MUTED = colors.HexColor("#BAB7B5")

PAGE_W, PAGE_H = A4
MARGIN_X = 22 * mm
MARGIN_TOP = 24 * mm
MARGIN_BOTTOM = 22 * mm
CONTENT_W = PAGE_W - 2 * MARGIN_X


def register_fonts() -> None:
    faces = {
        "Plex": "IBMPlexSans-400.ttf",
        "Plex-Medium": "IBMPlexSans-500.ttf",
        "Plex-SemiBold": "IBMPlexSans-600.ttf",
        "Plex-Bold": "IBMPlexSans-700.ttf",
        "PlexMono": "IBMPlexMono-400.ttf",
        "PlexMono-Medium": "IBMPlexMono-500.ttf",
    }
    for name, file in faces.items():
        pdfmetrics.registerFont(TTFont(name, str(FONTS / file)))
    pdfmetrics.registerFontFamily("Plex", normal="Plex", bold="Plex-SemiBold", italic="Plex", boldItalic="Plex-SemiBold")
    pdfmetrics.registerFontFamily("PlexMono", normal="PlexMono", bold="PlexMono-Medium", italic="PlexMono",
                                  boldItalic="PlexMono-Medium")


# ---------------------------------------------------------------- estilos
def styles() -> dict[str, ParagraphStyle]:
    base = ParagraphStyle("body", fontName="Plex", fontSize=9.6, leading=14.2, textColor=TEXT2, alignment=TA_LEFT,
                          spaceAfter=6)
    s = {
        "body": base,
        "eyebrow": ParagraphStyle("eyebrow", parent=base, fontName="PlexMono-Medium", fontSize=8.5, leading=11,
                                  textColor=MUTED, spaceAfter=6),
        "h1": ParagraphStyle("h1", parent=base, fontName="Plex-SemiBold", fontSize=27, leading=32, textColor=INK,
                             spaceAfter=4),
        "lead": ParagraphStyle("lead", parent=base, fontSize=11, leading=16.5, textColor=MUTED, spaceAfter=14),
        "h2": ParagraphStyle("h2", parent=base, fontName="Plex-SemiBold", fontSize=15, leading=19, textColor=INK,
                             spaceBefore=14, spaceAfter=6),
        "h3": ParagraphStyle("h3", parent=base, fontName="Plex-SemiBold", fontSize=11, leading=15, textColor=INK,
                             spaceBefore=10, spaceAfter=4),
        "bullet": ParagraphStyle("bullet", parent=base, leftIndent=13, bulletIndent=2, spaceAfter=3.5),
        "step": ParagraphStyle("step", parent=base, leftIndent=17, bulletIndent=0, spaceAfter=3.5),
        "cell": ParagraphStyle("cell", parent=base, fontSize=8.8, leading=12.4, spaceAfter=0),
        "cellhead": ParagraphStyle("cellhead", parent=base, fontName="Plex-SemiBold", fontSize=8.2, leading=11,
                                   textColor=ON_DARK, spaceAfter=0),
        "caption": ParagraphStyle("caption", parent=base, fontSize=8.3, leading=11.5, textColor=MUTED, spaceBefore=5,
                                  spaceAfter=12),
        "note": ParagraphStyle("note", parent=base, fontSize=9.1, leading=13.2, textColor=TEXT2, spaceAfter=0),
        "code": ParagraphStyle("code", parent=base, fontName="PlexMono", fontSize=8.1, leading=11.4, textColor=INK,
                               spaceAfter=0),
        "toc0": ParagraphStyle("toc0", parent=base, fontName="Plex-SemiBold", fontSize=10.5, leading=14,
                               textColor=INK, leftIndent=0, spaceBefore=8, spaceAfter=1),
        "toc1": ParagraphStyle("toc1", parent=base, fontSize=8.9, leading=12.2, textColor=TEXT2, leftIndent=29,
                               spaceAfter=0),
    }
    return s


S: dict[str, ParagraphStyle] = {}


# ---------------------------------------------------------------- marcação simples
def md(text: str) -> str:
    """**negrito**, `código`, [[tecla]] e › para caminhos de menu."""
    t = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    t = re.sub(r"\[\[(.+?)\]\]",
               lambda m: f'<font face="PlexMono-Medium" size="8.2" color="#000000" backColor="#E8E8E8">'
                         f'&nbsp;{m.group(1)}&nbsp;</font>', t)
    t = re.sub(r"`(.+?)`", lambda m: f'<font face="PlexMono" size="8.6" color="#2B2727">{m.group(1)}</font>', t)
    t = re.sub(r"\*\*(.+?)\*\*", r"<b>\1</b>", t)
    return t


# ---------------------------------------------------------------- flowables
class Rule(Flowable):
    def __init__(self, width: float, color=MAUVE, thickness: float = 1.6, space: float = 14):
        super().__init__()
        self.width, self.color, self.thickness, self.space = width, color, thickness, space

    def wrap(self, aw, ah):
        return self.width, self.thickness + self.space

    def draw(self):
        self.canv.setFillColor(self.color)
        self.canv.rect(0, self.space, self.width, self.thickness, stroke=0, fill=1)


class Heading(Paragraph):
    """Parágrafo que entra no sumário e vira marcador do PDF."""

    def __init__(self, text: str, style: ParagraphStyle, level: int, toc_text: str | None = None,
                 number: str | None = None):
        super().__init__(md(text), style)
        self.level = level
        self.toc_text = toc_text or text
        self.number = number


class Callout(Flowable):
    def __init__(self, label: str, text: str, tone: str = "tip"):
        super().__init__()
        self.tone = tone
        self.para = Paragraph(f"<b>{md(label)}</b> {md(text)}", S["note"])

    def wrap(self, aw, ah):
        self.aw = aw
        w, h = self.para.wrap(aw - 26, ah)
        self.h = h + 18
        return aw, self.h + 8

    def draw(self):
        c = self.canv
        bg = SAND if self.tone == "warn" else SURFACE2
        bar = BLACK if self.tone == "warn" else MAUVE
        c.setFillColor(bg)
        c.roundRect(0, 8, self.aw, self.h, 3, stroke=0, fill=1)
        c.setFillColor(bar)
        c.rect(0, 8, 3, self.h, stroke=0, fill=1)
        self.para.drawOn(c, 15, 8 + 9)


class CodeBlock(Flowable):
    def __init__(self, text: str):
        super().__init__()
        self.lines = text.strip("\n").split("\n")

    def wrap(self, aw, ah):
        self.aw = aw
        self.h = len(self.lines) * 11.4 + 18
        return aw, self.h + 10

    def draw(self):
        c = self.canv
        c.setFillColor(BLACK)
        c.roundRect(0, 10, self.aw, self.h, 4, stroke=0, fill=1)
        c.setFont("PlexMono", 8.1)
        y = 10 + self.h - 9 - 8.1
        for line in self.lines:
            c.setFillColor(ON_DARK)
            c.drawString(12, y, line)
            y -= 11.4


def logo(c, x: float, y: float, size: float, bg=BLACK, drawer=ON_DARK, led=BLACK) -> None:
    """Marca do Talos.: rack com três gavetas."""
    c.saveState()
    c.setFillColor(bg)
    c.rect(x, y, size, size, stroke=0, fill=1)
    dw, dh = size * 0.46, size * 0.098
    gap = size * 0.034
    total = 3 * dh + 2 * gap
    x0 = x + (size - dw) / 2
    y0 = y + (size - total) / 2
    for i in range(3):
        yy = y0 + i * (dh + gap)
        c.setFillColor(drawer)
        c.rect(x0, yy, dw, dh, stroke=0, fill=1)
        c.setFillColor(bg)
        c.rect(x0 + dw * 0.11, yy + dh * 0.4, dw * 0.44, dh * 0.22, stroke=0, fill=1)
        c.setFillColor(led)
        c.circle(x0 + dw * 0.84, yy + dh / 2, dh * 0.2, stroke=0, fill=1)
    c.restoreState()


# ---------------------------------------------------------------- documento
class ManualDoc(BaseDocTemplate):
    def __init__(self, filename: str, **kw):
        super().__init__(filename, pagesize=A4, leftMargin=MARGIN_X, rightMargin=MARGIN_X, topMargin=MARGIN_TOP,
                         bottomMargin=MARGIN_BOTTOM, title="Talos. — Manual de uso", author="Talos.",
                         subject="Manual de uso e referência técnica", creator="Talos.", **kw)
        frame = Frame(MARGIN_X, MARGIN_BOTTOM, CONTENT_W, PAGE_H - MARGIN_TOP - MARGIN_BOTTOM, id="f",
                      leftPadding=0, rightPadding=0, topPadding=0, bottomPadding=0)
        self.addPageTemplates([
            PageTemplate("cover", frames=[frame], onPage=draw_cover),
            PageTemplate("page", frames=[frame], onPage=draw_page),
        ])
        self.chapter = ""
        self._seq = 0
        self._starts: list[tuple[int, str]] = []
        self._prev_starts: list[tuple[int, str]] = []

    def afterFlowable(self, f):
        if isinstance(f, Heading):
            self._seq += 1
            key = f"h{self._seq}"
            self.canv.bookmarkPage(key)
            self.canv.addOutlineEntry(f.toc_text, key, level=f.level, closed=f.level > 0)
            label = f.toc_text
            if f.number:
                label = f'<font face="PlexMono-Medium" color="#6B6262">{f.number}</font>&nbsp;&nbsp;&nbsp;{f.toc_text}'
            self.notify("TOCEntry", (f.level, label, self.page, key))
            if f.level == 0:
                self._starts.append((self.page, f.toc_text))

    def chapter_for(self, page: int) -> str:
        """Capítulo da página, usando o mapa da passada anterior do multiBuild."""
        name = ""
        for start, title in self._prev_starts:
            if start <= page:
                name = title
        return name

    def handle_documentBegin(self):
        self._seq = 0
        if self._starts:
            self._prev_starts = self._starts
        self._starts = []
        super().handle_documentBegin()


def draw_cover(c, doc) -> None:
    """Capa no estilo do manual 1.0.0: fundo preto, marca clara e faixa com a paleta no rodapé."""
    c.saveState()
    c.setFillColor(BLACK)
    c.rect(0, 0, PAGE_W, PAGE_H, stroke=0, fill=1)
    x = 24 * mm
    mark = 30 * mm
    y_mark = PAGE_H / 2 + 34 * mm
    logo(c, x, y_mark, mark, bg=ON_DARK, drawer=BLACK, led=ON_DARK)
    c.setFillColor(ON_DARK)
    c.setFont("PlexMono-Medium", 52)
    c.drawString(x + mark + 8 * mm, y_mark + 7 * mm, "Talos")
    c.setFillColor(MAUVE)
    c.drawString(x + mark + 8 * mm + pdfmetrics.stringWidth("Talos", "PlexMono-Medium", 52), y_mark + 7 * mm, ".")

    c.setFillColor(ON_DARK)
    c.setFont("Plex-SemiBold", 24)
    c.drawString(x, PAGE_H / 2 + 6 * mm, "Manual de uso")
    c.setFont("Plex", 12.5)
    c.drawString(x, PAGE_H / 2 - 4 * mm, "Gestão de serviços de TI, central de suporte e inventário de ativos")
    c.setFillColor(ON_DARK_MUTED)
    c.setFont("Plex", 9.5)
    c.drawString(x, PAGE_H / 2 - 12 * mm, "Funções, telas, abas, ferramentas e informações técnicas")
    c.setFillColor(MAUVE)
    c.rect(x, PAGE_H / 2 - 21 * mm, 28 * mm, 0.9 * mm, stroke=0, fill=1)

    c.setFont("PlexMono", 9)
    c.setFillColor(ON_DARK)
    c.drawString(x, 58 * mm, f"Versão {VERSION}  ·  Outubro de 2026  ·  Windows 10 e 11 (64 bits)")
    c.drawString(x, 52 * mm, "Java 21  ·  Spring Boot 4  ·  JavaFX 21  ·  Licença MIT")
    c.drawString(x, 46 * mm, "Copyright (©) 2026 Erik Pimenta")
    # faixa com a paleta
    band = [PAPER, SAND, STONE, MAUVE, DARK]
    w = PAGE_W / len(band)
    for k, col in enumerate(band):
        c.setFillColor(col)
        c.rect(k * w, 0, w + 0.5, 22 * mm, stroke=0, fill=1)
    c.restoreState()


def draw_page(c, doc) -> None:
    """Páginas internas: barra preta com bloco malva no topo e rodapé com versão e número."""
    c.saveState()
    c.setFillColor(SURFACE)
    c.rect(0, 0, PAGE_W, PAGE_H, stroke=0, fill=1)
    bar_h = 3.2 * mm
    c.setFillColor(MAUVE)
    c.rect(0, PAGE_H - bar_h, PAGE_W * 0.1, bar_h, stroke=0, fill=1)
    c.setFillColor(BLACK)
    c.rect(PAGE_W * 0.1, PAGE_H - bar_h, PAGE_W * 0.9, bar_h, stroke=0, fill=1)
    chapter = doc.chapter_for(doc.page)
    if chapter:
        c.setFont("Plex", 7.8)
        c.setFillColor(MUTED)
        c.drawRightString(PAGE_W - MARGIN_X, PAGE_H - 10 * mm, chapter)
    # rodapé
    c.setStrokeColor(LINE)
    c.setLineWidth(0.6)
    c.line(MARGIN_X, 14 * mm, PAGE_W - MARGIN_X, 14 * mm)
    c.setFont("PlexMono-Medium", 8)
    c.setFillColor(INK)
    c.drawString(MARGIN_X, 9.5 * mm, "Talos")
    c.setFillColor(MAUVE)
    c.drawString(MARGIN_X + pdfmetrics.stringWidth("Talos", "PlexMono-Medium", 8), 9.5 * mm, ".")
    c.setFont("Plex", 7.8)
    c.setFillColor(MUTED)
    c.drawString(MARGIN_X + pdfmetrics.stringWidth("Talos. ", "PlexMono-Medium", 8) + 2, 9.5 * mm,
                 f"Manual de uso  ·  versão {VERSION}")
    c.setFont("Plex-SemiBold", 8.5)
    c.setFillColor(INK)
    c.drawRightString(PAGE_W - MARGIN_X, 9.5 * mm, str(doc.page))
    c.restoreState()


# ---------------------------------------------------------------- blocos
class Figures:
    def __init__(self, dirs: list[Path]):
        self.dirs = dirs
        self.n = 0

    def find(self, names) -> Path | None:
        if isinstance(names, str):
            names = [names]
        for name in names:
            for d in self.dirs:
                p = d / name
                if p.suffix == "":
                    p = p.with_suffix(".png")
                if p.exists():
                    return p
        return None

    def block(self, names, caption: str, width_ratio: float = 1.0):
        path = self.find(names)
        if path is None:
            return []
        self.n += 1
        iw, ih = ImageReader(str(path)).getSize()
        w = CONTENT_W * width_ratio
        h = w * ih / iw
        max_h = 150 * mm
        if h > max_h:
            h = max_h
            w = h * iw / ih
        img = Image(str(path), width=w, height=h)
        frame = Table([[img]], colWidths=[w], rowHeights=[h])
        frame.setStyle(TableStyle([
            ("BOX", (0, 0), (-1, -1), 0.6, LINE2),
            ("LEFTPADDING", (0, 0), (-1, -1), 0), ("RIGHTPADDING", (0, 0), (-1, -1), 0),
            ("TOPPADDING", (0, 0), (-1, -1), 0), ("BOTTOMPADDING", (0, 0), (-1, -1), 0),
        ]))
        frame.hAlign = "LEFT"
        cap = Paragraph(f'<font face="PlexMono-Medium" color="#000000">FIGURA {self.n:02d}</font>&nbsp;&nbsp;'
                        f'{md(caption)}', S["caption"])
        return [KeepTogether([Spacer(1, 4), frame, cap])]


def table(headers, rows, widths=None):
    n = len(headers)
    widths = widths or [1 / n] * n
    total = sum(widths)
    col_w = [CONTENT_W * w / total for w in widths]
    data = [[Paragraph(md(h), S["cellhead"]) for h in headers]]
    for r in rows:
        data.append([Paragraph(md(str(x)), S["cell"]) for x in r])
    t = Table(data, colWidths=col_w, repeatRows=1, hAlign="LEFT")
    t.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (-1, 0), HEAD),
        ("ROWBACKGROUNDS", (0, 1), (-1, -1), [SURFACE, ZEBRA]),
        ("LINEBELOW", (0, -1), (-1, -1), 0.6, LINE2),
        ("VALIGN", (0, 0), (-1, -1), "TOP"),
        ("LEFTPADDING", (0, 0), (-1, -1), 7), ("RIGHTPADDING", (0, 0), (-1, -1), 7),
        ("TOPPADDING", (0, 0), (-1, -1), 5.5), ("BOTTOMPADDING", (0, 0), (-1, -1), 5.5),
        ("TOPPADDING", (0, 0), (-1, 0), 6), ("BOTTOMPADDING", (0, 0), (-1, 0), 6),
    ]))
    return [Spacer(1, 3), t, Spacer(1, 10)]


def render(blocks, figs: Figures):
    out = []
    for b in blocks:
        kind = b[0]
        if kind == "p":
            out.append(Paragraph(md(b[1]), S["body"]))
        elif kind == "h2":
            out += [CondPageBreak(62 * mm), Heading(b[1], S["h2"], 1)]
        elif kind == "h3":
            out += [CondPageBreak(28 * mm), Paragraph(md(b[1]), S["h3"])]
        elif kind == "bullets":
            bx = ParagraphStyle("bx", parent=S["bullet"], bulletFontName="Plex", bulletFontSize=5.5,
                                bulletColor=INK, bulletOffsetY=-1.6)
            for item in b[1]:
                out.append(Paragraph(md(item), bx, bulletText="■"))
            out.append(Spacer(1, 4))
        elif kind == "steps":
            st = ParagraphStyle("sx", parent=S["step"], bulletFontName="PlexMono-Medium", bulletFontSize=8.6,
                                bulletColor=MUTED)
            for i, item in enumerate(b[1], 1):
                out.append(Paragraph(md(item), st, bulletText=f"{i:02d}"))
            out.append(Spacer(1, 4))
        elif kind == "table":
            out += table(b[1], b[2], b[3] if len(b) > 3 else None)
        elif kind == "fig":
            out += figs.block(b[1], b[2], b[3] if len(b) > 3 else 1.0)
        elif kind == "note":
            out += [Callout(b[1], b[2], b[3] if len(b) > 3 else "tip"), Spacer(1, 4)]
        elif kind == "code":
            out.append(CodeBlock(b[1]))
        elif kind == "break":
            out.append(PageBreak())
        else:
            raise ValueError(kind)
    return out


def build(images: Path | None, out: Path) -> None:
    register_fonts()
    S.update(styles())
    dirs = [d for d in [images, ROOT / "docs/images"] if d is not None]
    figs = Figures(dirs)
    doc = ManualDoc(str(out))

    story = [NextPageTemplate("page"), PageBreak()]
    # sumário
    story += [Paragraph("Sumário", S["h1"]), Rule(28 * mm), Spacer(1, 4)]
    toc = TableOfContents()
    toc.levelStyles = [S["toc0"], S["toc1"]]
    toc.dotsMinLevel = 99  # sem pontilhado: números alinhados à direita
    story.append(toc)

    for ch in chapters():
        story.append(PageBreak())
        story.append(Paragraph(f"{ch['number']} · {ch['eyebrow'].upper()}", S["eyebrow"]))
        story.append(Heading(ch["title"], S["h1"], 0, number=ch["number"]))
        story.append(Rule(28 * mm))
        if ch.get("lead"):
            story.append(Paragraph(md(ch["lead"]), S["lead"]))
        story += render(ch["blocks"], figs)

    doc.multiBuild(story)
    print(f"manual gerado: {out}  ({figs.n} figuras)")


def main() -> None:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--images", type=Path, default=None, help="pasta com os prints (ScreenshotTour)")
    ap.add_argument("--out", type=Path, default=ROOT / f"Manual-Talos-Versão {VERSION}.pdf")
    a = ap.parse_args()
    build(a.images, a.out)


if __name__ == "__main__":
    main()
