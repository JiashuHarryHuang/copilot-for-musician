import os
import glob
from pdf2image import convert_from_path
from dotenv import load_dotenv
from PIL import Image
import io
import dashscope
import tempfile

# Load environment variables
load_dotenv(os.path.join(os.path.dirname(__file__), '../.env'))
API_KEY = os.getenv('DASHSCOPE_API_KEY')

dashscope.api_key = API_KEY

PDF_DIR = os.path.join(os.path.dirname(__file__), '../music-document')
SUMMARY_SUFFIX = '_summary.md'


def pdf_to_images(pdf_path):
    # Convert PDF to a list of PIL Images
    return convert_from_path(pdf_path)


def image_to_bytes(img):
    # Convert PIL Image to bytes (JPEG)
    buf = io.BytesIO()
    img.save(buf, format='JPEG')
    buf.seek(0)
    return buf.read()


def summarize_image_with_sdk(image_bytes, page_num=None, pdf_name=None):
    import time
    import traceback
    start_time = time.time()
    try:
        with tempfile.NamedTemporaryFile(suffix='.jpg', delete=True) as tmp:
            tmp.write(image_bytes)
            tmp.flush()
            response = dashscope.MultiModalConversation.call(
                model='qwen-vl-plus',
                messages=[
                    {
                        'role': 'user',
                        'content': [
                            {'image': tmp.name},
                            {'text': 'Summarize the content of this music document page in English.'}
                        ]
                    }
                ],
                timeout=60  # 60 seconds timeout
            )
            if hasattr(response, 'output') and 'choices' in response.output:
                return response.output['choices'][0]['message']['content'][0]['text']
            return 'No summary returned.'
    except Exception as e:
        print(f"[ERROR] Failed to summarize page {page_num} of {pdf_name}: {e}")
        traceback.print_exc()
        return f'Error summarizing page {page_num}: {e}'
    finally:
        print(f"    [INFO] Page {page_num} processed in {time.time() - start_time:.1f}s")


def process_pdf(pdf_path):
    print(f'Processing {pdf_path}...')
    images = pdf_to_images(pdf_path)
    summaries = []
    pdf_name = os.path.basename(pdf_path)
    summaries.append(f'# Summary for {pdf_name}\n')
    for i, img in enumerate(images):
        page_num = i + 1
        print(f'  Summarizing page {page_num}/{len(images)}...')
        img_bytes = image_to_bytes(img)
        summary = summarize_image_with_sdk(img_bytes, page_num=page_num, pdf_name=pdf_name)
        summaries.append(f'\n## Page {page_num}\n{summary}\n')
    return '\n'.join(summaries)


def main():
    pdf_files = glob.glob(os.path.join(PDF_DIR, '*.pdf'))
    for pdf_file in pdf_files:
        base = os.path.splitext(os.path.basename(pdf_file))[0]
        summary_path = os.path.join(PDF_DIR, f'{base}{SUMMARY_SUFFIX}')
        summary = process_pdf(pdf_file)
        with open(summary_path, 'w', encoding='utf-8') as f:
            f.write(summary)
        print(f'Saved summary to {summary_path}')


if __name__ == '__main__':
    main()
