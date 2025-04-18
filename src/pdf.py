import pathlib, os, io, logging
from openai import OpenAI
import pandas as pd
import pymupdf4llm
import io
import json
import json
from pathlib import Path
import pandas as pd

loglevel = os.getenv("LOGLEVEL", "WARNING").upper()
logging.basicConfig(level=loglevel)

OPENAI_API_KEY = os.getenv("OPENAI_API_KEY")
if not OPENAI_API_KEY:
    raise EnvironmentError("OPENAI_API_KEY environment variable is not set.")
client = OpenAI(api_key=OPENAI_API_KEY)

def convert_pdf_to_markdown(input_pdf_path, output_md_path):

    # Convert PDF to Markdown
    md_text = pymupdf4llm.to_markdown(input_pdf_path)

    # Save the Markdown text to the output file
    pathlib.Path(output_md_path).write_bytes(md_text.encode())

# Example usage
# input_pdf_path = '/home/doehyunbaek/artifex/evaluation/papers/Unimocg.pdf'
# output_md_path = '/home/doehyunbaek/artifex/evaluation/output.md'
# convert_pdf_to_markdown(input_pdf_path, output_md_path)



def extract_tables_from_markdown(input_md_path, output_js_path):
    # Read the Markdown content
    md_text = pathlib.Path(input_md_path).read_text()

    # Define the prompt
        # ...existing code...
        # Define the prompt
    prompt = (
            "Extract ONLY the explicit tables from the given markdown document that follow the structure of 'Table 1: Soundness of call-graphs for different JVM features', "
            "or similar, where tables are presented as aligned columns of text (not markdown syntax tables). "
            "Ignore any tables written in markdown syntax (i.e., those starting with '|' or containing markdown table formatting). "
            "Do NOT extract tables that use markdown syntax. "
            "Each table should be a separate JSON object within the array. For each table, include the following keys: "
            "'headers' (an array of column names), 'rows' (an array of arrays, where each inner array represents a row of data), "
            "and 'footer' (an object containing any footer rows, such as 'Sum', if present). "
            "If the number of columns in a row or footer does not match the number of headers, "
            "intelligently infer where each value should fit based on context. "
            "Insert null for any missing values so that all rows and footers align with the headers. "
            "Ensure that the JSON output is well-structured and does not include any additional text, explanations, or formatting. "
            "Only return the raw JSON array:\n\n"
            f"{md_text}"
        )
    # ...existing code...
    # Prepare the messages for the ChatCompletion API
    messages = [
        {"role": "system", "content": "You are a helpful assistant."},
        {"role": "user", "content": prompt}
    ]

    # Call OpenAI API
    response = client.chat.completions.create(
        model="gpt-4.1", # TODO: use cheaper models
        messages=messages,
        max_tokens=32768,
        temperature=0
    )

    # Extract the response text
    logging.debug(f'response:\n{response}')
    tables_text = response.choices[0].message.content.strip()

    # Parse the JSON response
    try:
        tables = json.loads(tables_text)
        if not isinstance(tables, list):  # Ensure the response is a list
            raise ValueError("The response JSON is not a list of tables.")
    except (json.JSONDecodeError, ValueError) as e:
        logging.error(f"Error decoding JSON or invalid format: {e}")
        return None

    # Write the JSON tables to the output file
    pathlib.Path(output_js_path).write_text(json.dumps(tables, indent=4))

# def adjust_table_headers_footer(input_json_path):
#     # Read the Markdown content
#     json_text = json.loads(input_json_path)

#     # Define the prompt
#     prompt = (
#         "Extract all the tables from the given markdown document and return them as a JSON array. "
#         "Each table should be a separate JSON object within the array. For each table, include the following keys: "
#         "'headers' (an array of column names), 'rows' (an array of arrays, where each inner array represents a row of data), "
#         "and 'footer' (an object containing any footer rows, such as 'Sum', if present). "
#         "Ensure that the JSON output is well-structured and does not include any additional text, explanations, or formatting. "
#         "Only return the raw JSON array:\n\n"
#         f"{json_text}"
#     )
#     # Prepare the messages for the ChatCompletion API
#     messages = [
#         {"role": "system", "content": "You are a helpful assistant."},
#         {"role": "user", "content": prompt}
#     ]

#     # Call OpenAI API
#     response = client.chat.completions.create(
#         model="gpt-4.1",
#         messages=messages,
#         max_tokens=32768,
#         temperature=0
#     )

#     # Extract the response text
#     logging.debug(f'response:\n{response}')
#     tables_text = response.choices[0].message.content.strip()

#     # Parse the JSON response
#     try:
#         tables = json.loads(tables_text)
#         if not isinstance(tables, list):  # Ensure the response is a list
#             raise ValueError("The response JSON is not a list of tables.")
#     except (json.JSONDecodeError, ValueError) as e:
#         logging.error(f"Error decoding JSON or invalid format: {e}")
#         return None

#     # Write the JSON tables to the output file
#     pathlib.Path(input_json_path).write_text(json.dumps(tables, indent=4))    

def draw_tables_from_json(input_json_path: str | Path, output_table_path: str | Path):
    input_json_path = Path(input_json_path).expanduser().resolve()
    with input_json_path.open(encoding="utf-8") as fh:
        raw = json.load(fh)

    if not isinstance(raw, list):
        raise ValueError("Expected the JSON root to be a list")

    tables = []
    for i, obj in enumerate(raw):
        df = pd.DataFrame(obj["rows"], columns=obj["headers"])
        footer = obj.get("footer")
        # If footer is a dict, append as a row to the DataFrame
        if isinstance(footer, dict):
            # Ensure the footer dict has all columns, fill missing with empty string
            footer_row = [footer.get(col, "") for col in obj["headers"]]
            df = pd.concat([df, pd.DataFrame([footer_row], columns=obj["headers"])], ignore_index=True)
        tables.append((f"table_{i+1}", df))

    # Set Pandas options to display everything
    pd.set_option("display.max_rows", None)
    pd.set_option("display.max_columns", None)
    pd.set_option("display.width", None)

    with open(output_table_path, "w", encoding="utf-8") as output_file:
        for name, df in tables:
            output_file.write(f"\n===== {name} =====\n")
            output_file.write(df.to_string(index=False))
            output_file.write("\n")

# Example usage
input_md_path = '/home/doehyunbaek/artifex/evaluation/output.md'
json_path = '/home/doehyunbaek/artifex/evaluation/output.json'
table_path = "/home/doehyunbaek/artifex/evaluation/output.tables"
extract_tables_from_markdown(input_md_path, json_path)
draw_tables_from_json(json_path, table_path)
