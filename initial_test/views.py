from drf_spectacular.utils import extend_schema
from rest_framework import status
from rest_framework.decorators import api_view
from rest_framework.response import Response

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


@extend_schema(
    methods=["GET"],
    responses=TestRestGetResponseSerializer,
)
@extend_schema(
    methods=["POST"],
    request=TestRestRequestSerializer,
    responses=TestRestResponseSerializer,
)
@api_view(["GET", "POST"])
def test_rest(request):
    if request.method == "GET":
        return Response({"message": "You called me!"})

    if request.method == "POST":
        try:
            serializer = TestRestRequestSerializer(data=request.data)
            serializer.is_valid(raise_exception=True)
            response_data = {"response": serializer.validated_data["message"]}
            return Response(response_data, status=status.HTTP_200_OK)
        except:
            return Response({"error": "invalid request"}, status=status.HTTP_400_BAD_REQUEST)
