import os

import requests
from django.conf import settings
from django.db import IntegrityError, transaction
from drf_spectacular.utils import OpenApiResponse, extend_schema, extend_schema_view
from rest_framework import status, viewsets
from rest_framework.decorators import action
from rest_framework.exceptions import ValidationError
from rest_framework.permissions import AllowAny
from rest_framework.response import Response

from .models import User
from .serializers import LoginRequestSerializer, LoginResponseSerializer, UserCreateSerializer, UserSerializer


@extend_schema_view(
    list=extend_schema(
        summary="사용자 목록 조회(실제로 안 씀)",
        description="전체 사용자 목록을 조회합니다.",
        request="",
        responses={200: OpenApiResponse(response=UserSerializer, description="OK")},
        tags=["User"],
    ),
    retrieve=extend_schema(
        summary="특정 사용자 조회(실제로 안 씀)",
        description="ID로 특정 사용자의 상세 정보를 조회합니다.",
        responses={
            200: OpenApiResponse(UserSerializer, description="OK"),
            404: OpenApiResponse(description="Not Found"),
        },
        tags=["User"],
    ),
    create=extend_schema(
        summary="사용자 생성(회원가입 대체)",
        description="새로운 사용자를 생성합니다.",
        request=UserCreateSerializer,
        responses={
            201: OpenApiResponse(UserSerializer, description="Created"),
            400: OpenApiResponse(description="Bad Request"),
        },
        tags=["User"],
    ),
    update=extend_schema(
        summary="사용자 정보 전체 수정 (PUT)",
        description="사용자의 모든 필드를 갱신합니다.",
        request=UserSerializer,
        responses={
            200: OpenApiResponse(UserSerializer, description="OK"),
            400: OpenApiResponse(description="Bad Request"),
            404: OpenApiResponse(description="Not Found"),
        },
        tags=["User"],
    ),
    partial_update=extend_schema(
        summary="사용자 정보 부분 수정 (PATCH)",
        description="사용자의 일부 필드만 부분 갱신합니다.",
        request=UserSerializer,
        responses={
            200: OpenApiResponse(UserSerializer, description="OK"),
            400: OpenApiResponse(description="Bad Request"),
            404: OpenApiResponse(description="Not Found"),
        },
        tags=["User"],
    ),
    destroy=extend_schema(
        summary="사용자 삭제",
        description="ID로 특정 사용자를 삭제합니다.",
        responses={204: OpenApiResponse(description="No Content"), 404: OpenApiResponse(description="Not Found")},
        tags=["User"],
    ),
    login=extend_schema(
        summary="로그인",
        description="username을 입력하면 user_pk를 반환함",
        tags=["User"],
        request=LoginRequestSerializer,
        responses={200: LoginResponseSerializer},
    ),
)
class UserViewSet(viewsets.ModelViewSet):
    queryset = User.objects.all()
    serializer_class = UserSerializer

    @transaction.atomic
    def perform_create(self, serializer):
        try:
            user = serializer.save()
        except Exception:
            raise ValidationError("요청 형식 오류")

        if not os.getenv("FINAPI_SECRET"):
            raise ValidationError({"external": "서버 설정 오류(FINAPI_SECRET 미설정)."})

        try:
            resp = requests.post(
                "https://finopenapi.ssafy.io/ssafy/api/v1/member/",
                json={"apiKey": os.getenv("FINAPI_SECRET"), "userId": user.email},
                timeout=5,
            )
        except requests.RequestException as e:
            raise ValidationError({"external": f"finopenapi 호출 실패: {e}"})

        if resp.status_code in (200, 201):
            try:
                payload = resp.json()
            except ValueError:
                raise ValidationError({"external": "외부 응답 파싱 실패(JSON 아님)."})

            user_key = payload.get("userKey")
            if not user_key:
                raise ValidationError({"external": "외부 응답에 userKey가 없습니다."})

            user.user_key = user_key
            user.save(update_fields=["user_key"])
            return

        # Handle cases where the user might already exist in the external system (400 or 409)
        if resp.status_code in (400, 409):
            try:
                search_resp = requests.post(
                    "https://finopenapi.ssafy.io/ssafy/api/v1/member/search",
                    json={"apiKey": os.getenv("FINAPI_SECRET"), "userId": user.email},
                    timeout=5,
                )
            except requests.RequestException as e:
                raise ValidationError({"external": f"finopenapi search call failed: {e}"})

            if search_resp.status_code in (200, 201):
                try:
                    payload = search_resp.json()
                except ValueError:
                    raise ValidationError({"external": "External search response parsing failed (not JSON)."})

                user_key = payload.get("userKey")
                if not user_key:
                    message = payload.get("responseMessage", "User not found via search after initial create failed.")
                    raise ValidationError({"external": message})

                user.user_key = user_key
                user.save(update_fields=["user_key"])
                return
            else:
                # The search request itself failed, which is unexpected if the user exists.
                # Provide a detailed error message for debugging.
                error_body = ""
                try:
                    # Try to get the JSON response for a more structured error message.
                    error_body = search_resp.json()
                except ValueError:
                    # If the response is not JSON, use the raw text.
                    error_body = search_resp.text

                message = (
                    f"Search API call failed with status {search_resp.status_code}. "
                    f"This is unexpected because the initial creation failed, implying the user already exists. "
                    f"Response body: {error_body}"
                )
                raise ValidationError({"external": message})

        if resp.status_code in (401, 403):
            raise ValidationError({"external": "외부 인증 실패(서버 설정 확인 필요)."})

        raise ValidationError({"external": f"예상치 못한 상태코드: {resp.status_code}"})

    @action(detail=False, methods=["post"], url_path="login", permission_classes=[AllowAny])
    def login(self, request):
        in_ser = LoginRequestSerializer(data=request.data)
        if not in_ser.is_valid():
            return Response(in_ser.errors, status=status.HTTP_400_BAD_REQUEST)

        email = (in_ser.validated_data or {}).get("email")
        try:
            user = User.objects.get(email=email)
        except User.DoesNotExist:
            return Response({"detail": "해당 이메일의 사용자가 존재하지 않습니다."}, status=status.HTTP_404_NOT_FOUND)

        out_ser = LoginResponseSerializer(user)
        return Response(out_ser.data, status=status.HTTP_200_OK)
