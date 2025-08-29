from drf_spectacular.utils import extend_schema
from rest_framework import status
from rest_framework.decorators import api_view, parser_classes
from rest_framework.parsers import MultiPartParser
from rest_framework.response import Response

from ledger.services import ocr_from_file

from .serializers import (
    PingSerializer,
    TestRestGetResponseSerializer,
    TestRestRequestSerializer,
    TestRestResponseSerializer,
)


@extend_schema(responses=PingSerializer)
@api_view(["GET"])
def ping(request):
    return Response({"response": "pong"})


@extend_schema(methods=["POST"], request=TestRestRequestSerializer, responses=TestRestResponseSerializer)
@api_view(["POST"])
@parser_classes([MultiPartParser])
def test_ocr(request):
    try:
        image_file = request.FILES["file"]
        ocr_result = ocr_from_file(image_file)
        return Response(ocr_result)
    except KeyError:
        return Response({"error": "file is required"}, status=status.HTTP_400_BAD_REQUEST)
    except Exception as e:
        return Response({"error": str(e)}, status=status.HTTP_500_INTERNAL_SERVER_ERROR)
