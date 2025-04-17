import pathlib, os, io, logging
from openai import OpenAI
import pandas as pd
import pymupdf4llm
import io  # Import StringIO from the io module
import json  # Import JSON for parsing

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
    prompt = (
        "Extract all the tables from the given markdown document and return them as a JSON array. "
        "Each table should be a separate JSON object within the array. For each table, include the following keys: "
        "'headers' (an array of column names), 'rows' (an array of arrays, where each inner array represents a row of data), "
        "and 'summary' (an object containing any summary rows, such as 'Sum', if present). "
        "Ensure that the JSON output is well-structured and does not include any additional text, explanations, or formatting. "
        "Only return the raw JSON array:\n\n"
        f"{md_text}"
    )
    # Prepare the messages for the ChatCompletion API
    messages = [
        {"role": "system", "content": "You are a helpful assistant."},
        {"role": "user", "content": prompt}
    ]

    # Call OpenAI API
    response = client.chat.completions.create(
        model="gpt-4.1-mini",  # Use the appropriate model
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

    # Return the parsed JSON tables
    return tables

# Example usage
input_md_path = '/home/doehyunbaek/artifex/evaluation/output.md'
output_js_path = '/home/doehyunbaek/artifex/evaluation/output.json'
tables = extract_tables_from_markdown(input_md_path, output_js_path)

# Print a success message
if tables is not None:
    print(f"Extracted tables have been saved to {output_js_path}")
else:
    print("No valid tables were extracted.")