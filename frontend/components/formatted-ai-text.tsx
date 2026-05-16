type FormattedAiTextProps = {
  text: string;
};

type Block =
  | {
      type: "heading";
      text: string;
    }
  | {
      type: "paragraph";
      text: string;
    }
  | {
      type: "unordered-list";
      items: string[];
    }
  | {
      type: "ordered-list";
      items: string[];
    };

export function FormattedAiText({ text }: FormattedAiTextProps) {
  const blocks = parseBlocks(text);

  return (
    <div className="formatted-ai-text">
      {blocks.map((block, index) => {
        if (block.type === "heading") {
          return (
            <h3 className="formatted-heading" key={index}>
              {renderInlineMarkdown(block.text)}
            </h3>
          );
        }

        if (block.type === "unordered-list") {
          return (
            <ul className="formatted-list" key={index}>
              {block.items.map((item, itemIndex) => (
                <li key={itemIndex}>{renderInlineMarkdown(item)}</li>
              ))}
            </ul>
          );
        }

        if (block.type === "ordered-list") {
          return (
            <ol className="formatted-list" key={index}>
              {block.items.map((item, itemIndex) => (
                <li key={itemIndex}>{renderInlineMarkdown(item)}</li>
              ))}
            </ol>
          );
        }

        return (
          <p className="formatted-paragraph" key={index}>
            {renderInlineMarkdown(block.text)}
          </p>
        );
      })}
    </div>
  );
}

function parseBlocks(text: string) {
  const lines = text.replace(/\r\n/g, "\n").split("\n");
  const blocks: Block[] = [];
  let paragraphLines: string[] = [];
  let unorderedItems: string[] = [];
  let orderedItems: string[] = [];

  function flushParagraph() {
    if (paragraphLines.length === 0) {
      return;
    }

    blocks.push({
      type: "paragraph",
      text: paragraphLines.join(" "),
    });
    paragraphLines = [];
  }

  function flushUnorderedList() {
    if (unorderedItems.length === 0) {
      return;
    }

    blocks.push({
      type: "unordered-list",
      items: unorderedItems,
    });
    unorderedItems = [];
  }

  function flushOrderedList() {
    if (orderedItems.length === 0) {
      return;
    }

    blocks.push({
      type: "ordered-list",
      items: orderedItems,
    });
    orderedItems = [];
  }

  function flushLists() {
    flushUnorderedList();
    flushOrderedList();
  }

  for (const line of lines) {
    const trimmedLine = line.trim();

    if (!trimmedLine) {
      flushParagraph();
      flushLists();
      continue;
    }

    const headingMatch = trimmedLine.match(/^#{1,6}\s*(.+)$/);
    if (headingMatch) {
      flushParagraph();
      flushLists();
      blocks.push({
        type: "heading",
        text: stripLeadingNumber(headingMatch[1]),
      });
      continue;
    }

    const unorderedMatch = trimmedLine.match(/^[-*]\s+(.+)$/);
    if (unorderedMatch) {
      flushParagraph();
      flushOrderedList();
      unorderedItems.push(unorderedMatch[1]);
      continue;
    }

    const orderedMatch = trimmedLine.match(/^\d+[.)]\s+(.+)$/);
    if (orderedMatch) {
      flushParagraph();
      flushUnorderedList();
      orderedItems.push(orderedMatch[1]);
      continue;
    }

    flushLists();
    paragraphLines.push(trimmedLine);
  }

  flushParagraph();
  flushLists();

  return blocks;
}

function stripLeadingNumber(text: string) {
  return text.replace(/^\d+[.)]\s*/, "");
}

function renderInlineMarkdown(text: string) {
  const parts = text.split(/(\*\*[^*]+\*\*)/g);

  return parts.map((part, index) => {
    if (part.startsWith("**") && part.endsWith("**")) {
      return <strong key={index}>{part.slice(2, -2)}</strong>;
    }

    return part;
  });
}
