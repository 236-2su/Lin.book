import os

import requests

# def get_receipt(file):
#     clova_api_url = os.getenv("CLOVA_API_URL")
#     secret_key = os.getenv("CLOVA_API_SECRET")
#     try:
#         request_json = {
#             'images': [
#                 {
#                     'format': 'jpg',
#                     'name': 'demo'
#                 }
#                 ],
#                 'requestId': str(uuid.uuid4()),
#                 'version': 'V2',
#                 'timestamp': int(round(time.time() * 1000))
#             }
#         payload = {'message': json.dumps(request_json).encode('UTF-8')}
#         files = [
#         ('file', open(file,'rb'))
#             ]
#         headers = {
#         'X-OCR-SECRET': secret_key
#         }
#         response = requests.request("POST", clova_api_url, headers=headers, data = payload, files = files)

#             for i in response.json()['images'][0]['fields']:
#                 receipt_data.append(i['inferText'])
#     except Exception as e:
#         tb_msg.setPlainText(str(e))
