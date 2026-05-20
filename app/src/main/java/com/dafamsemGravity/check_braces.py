import os

def check_braces(filepath):
    if not os.path.exists(filepath):
        print(f"File not found: {filepath}")
        return

    with open(filepath, 'r', encoding='utf-8') as f:
        lines = f.readlines()

    stack = []
    in_string = False
    in_char = False
    in_line_comment = False
    in_block_comment = False
    escape = False

    for line_idx, line in enumerate(lines):
        line_num = line_idx + 1
        i = 0
        n = len(line)
        while i < n:
            char = line[i]

            if in_line_comment:
                # Line comments end at newline
                break

            if in_block_comment:
                if char == '*' and i + 1 < n and line[i+1] == '/':
                    in_block_comment = False
                    i += 2
                    continue
                i += 1
                continue

            if escape:
                escape = False
                i += 1
                continue

            if char == '\\':
                escape = True
                i += 1
                continue

            if in_string:
                if char == '"':
                    in_string = False
                i += 1
                continue

            if in_char:
                if char == "'":
                    in_char = False
                i += 1
                continue

            # Check comments
            if char == '/' and i + 1 < n:
                if line[i+1] == '/':
                    in_line_comment = True
                    break
                elif line[i+1] == '*':
                    in_block_comment = True
                    i += 2
                    continue

            # Check string or char literals
            if char == '"':
                in_string = True
                i += 1
                continue
            if char == "'":
                in_char = True
                i += 1
                continue

            # Brace tracking
            if char == '{':
                stack.append(('{', line_num, i + 1))
            elif char == '}':
                if not stack:
                    print(f"Extra closing brace '}}' at line {line_num}, col {i + 1}")
                else:
                    stack.pop()

            i += 1
        in_line_comment = False  # Reset for next line

    if stack:
        print(f"Unclosed braces: {len(stack)}")
        for item in stack:
            print(f"  Unclosed '{item[0]}' opened at line {item[1]}, col {item[2]}")
    else:
        print("All braces are perfectly balanced!")

if __name__ == "__main__":
    check_braces("/Users/ag/Project/HTV/app/src/main/java/com/dafamsemarang/dhtv/HomeScreen.kt")
