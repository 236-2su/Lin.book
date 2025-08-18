from rest_framework.response import Response
from rest_framework import status
from rest_framework.decorators import api_view


@api_view(["GET"])
def ping(request):
    return Response({"response": "pong"})


@api_view(["GET", "POST"])
def test_rest(request):
    if request.method == "GET":
        return Response({"message": "You called me!"})

    if request.method == "POST":
        try:
            response = request.data.get("message")
            return Response({"response": response}, status=status.HTTP_200_OK)
        except:
            return Response(
                {"error": "invalid request"}, status=status.HTTP_400_BAD_REQUEST
            )
