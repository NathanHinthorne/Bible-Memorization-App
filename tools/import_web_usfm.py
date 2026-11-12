#!/usr/bin/env python3
"""Convert the official WEB USFM archive into the app's compact TSV corpus."""

import argparse
import re
import zipfile
from pathlib import Path

BOOKS = [
    ("GEN", "Genesis"), ("EXO", "Exodus"), ("LEV", "Leviticus"),
    ("NUM", "Numbers"), ("DEU", "Deuteronomy"), ("JOS", "Joshua"),
    ("JDG", "Judges"), ("RUT", "Ruth"), ("1SA", "1 Samuel"),
    ("2SA", "2 Samuel"), ("1KI", "1 Kings"), ("2KI", "2 Kings"),
    ("1CH", "1 Chronicles"), ("2CH", "2 Chronicles"), ("EZR", "Ezra"),
    ("NEH", "Nehemiah"), ("EST", "Esther"), ("JOB", "Job"),
    ("PSA", "Psalms"), ("PRO", "Proverbs"), ("ECC", "Ecclesiastes"),
    ("SNG", "Song of Solomon"), ("ISA", "Isaiah"), ("JER", "Jeremiah"),
    ("LAM", "Lamentations"), ("EZK", "Ezekiel"), ("DAN", "Daniel"),
    ("HOS", "Hosea"), ("JOL", "Joel"), ("AMO", "Amos"), ("OBA", "Obadiah"),
    ("JON", "Jonah"), ("MIC", "Micah"), ("NAM", "Nahum"), ("HAB", "Habakkuk"),
    ("ZEP", "Zephaniah"), ("HAG", "Haggai"), ("ZEC", "Zechariah"),
    ("MAL", "Malachi"), ("MAT", "Matthew"), ("MRK", "Mark"), ("LUK", "Luke"),
    ("JHN", "John"), ("ACT", "Acts"), ("ROM", "Romans"),
    ("1CO", "1 Corinthians"), ("2CO", "2 Corinthians"), ("GAL", "Galatians"),
    ("EPH", "Ephesians"), ("PHP", "Philippians"), ("COL", "Colossians"),
    ("1TH", "1 Thessalonians"), ("2TH", "2 Thessalonians"),
    ("1TI", "1 Timothy"), ("2TI", "2 Timothy"), ("TIT", "Titus"),
    ("PHM", "Philemon"), ("HEB", "Hebrews"), ("JAS", "James"),
    ("1PE", "1 Peter"), ("2PE", "2 Peter"), ("1JN", "1 John"),
    ("2JN", "2 John"), ("3JN", "3 John"), ("JUD", "Jude"),
    ("REV", "Revelation"),
]

MARKER = re.compile(r"\\([cv])\s+([0-9]+(?:-[0-9]+)?[a-z]?)\b")


def clean_usfm(text: str) -> str:
    text = re.sub(r"\\[fx]\b.*?\\[fx]\*", " ", text, flags=re.DOTALL)
    text = re.sub(
        r"\\\+?w\s+([^|\\]+)\|[^\\]*?\\\+?w\*",
        lambda match: match.group(1),
        text,
    )
    text = re.sub(r"\\[a-zA-Z0-9+*-]+", " ", text)
    text = re.sub(r"\s+", " ", text).strip()
    text = re.sub(r"\s+([,.;:!?])", r"\1", text)
    return text


def parse_book(book_id: str, text: str) -> list[tuple[int, int, str]]:
    markers = list(MARKER.finditer(text))
    chapter = 0
    verses: dict[tuple[int, int], str] = {}
    for index, marker in enumerate(markers):
        kind, value = marker.groups()
        if kind == "c":
            chapter = int(value.split("-", 1)[0])
            continue

        verse = int(re.match(r"\d+", value).group())
        end = markers[index + 1].start() if index + 1 < len(markers) else len(text)
        passage = clean_usfm(text[marker.end():end])
        if not chapter or not passage:
            continue
        key = (chapter, verse)
        verses[key] = " ".join(part for part in (verses.get(key), passage) if part)

    return [(chapter, verse, text) for (chapter, verse), text in sorted(verses.items())]


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("archive", type=Path, help="Official engwebu_usfm.zip")
    parser.add_argument("output", type=Path, help="Destination web.tsv")
    args = parser.parse_args()

    with zipfile.ZipFile(args.archive) as archive:
        members = archive.namelist()
        by_id: dict[str, str] = {}
        for member in members:
            match = re.search(r"\d{2}-([A-Z0-9]{3})engwebu\.usfm$", member)
            if match:
                by_id[match.group(1)] = member

        missing = [book_id for book_id, _ in BOOKS if book_id not in by_id]
        if missing:
            raise RuntimeError(f"WEB archive is missing canonical books: {', '.join(missing)}")

        args.output.parent.mkdir(parents=True, exist_ok=True)
        verse_count = 0
        with args.output.open("w", encoding="utf-8", newline="\n") as output:
            for book_number, (book_id, _) in enumerate(BOOKS, start=1):
                source = archive.read(by_id[book_id]).decode("utf-8")
                for chapter, verse, text in parse_book(book_id, source):
                    output.write(f"{book_number}\t{chapter}\t{verse}\t{text}\n")
                    verse_count += 1

    print(f"Wrote {verse_count} verses from {len(BOOKS)} books to {args.output}")


if __name__ == "__main__":
    main()
