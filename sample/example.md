# md2pdf sample document

This document exercises the main Markdown features plus a couple of Mermaid
diagrams.

## Text formatting

Regular text with **bold**, *italic*, ~~strike-through~~ and `inline code`.
An autolink: https://github.com/mermaid-js/mermaid-cli

> A block quote to check the styling.

## A table

| Feature      | Supported |
|--------------|:---------:|
| Tables       | yes       |
| Task lists   | yes       |
| Mermaid      | yes       |

## A task list

- [x] Parse Markdown
- [x] Render Mermaid to PNG
- [ ] Conquer the world

## Flowchart

```mermaid
graph TD
    A[Start] --> B{Markdown has Mermaid?}
    B -- Yes --> C[Render with mmdc]
    B -- No --> D[Skip]
    C --> E[Embed PNG]
    D --> E
    E --> F[Generate PDF]
```

## Sequence diagram

```mermaid
sequenceDiagram
    participant U as User
    participant App as md2pdf
    participant M as mmdc
    U->>App: convert README.md
    App->>M: render diagram
    M-->>App: diagram.png
    App-->>U: README.pdf
```

## Code block

```java
public static void main(String[] args) {
    System.out.println("Hello, PDF!");
}
```
