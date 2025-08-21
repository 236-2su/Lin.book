from rest_framework import viewsets
from drf_spectacular.utils import (
    extend_schema_view,
    extend_schema,
    OpenApiResponse,
)

from .models import User
from .serializers import UserSerializer


@extend_schema_view(
    list=extend_schema(
        summary="사용자 목록 조회",
        description="전체 사용자 목록을 조회합니다.",
        responses={200: OpenApiResponse(response=UserSerializer, description="OK")},
        tags=["User"],
    ),
    retrieve=extend_schema(
        summary="특정 사용자 조회",
        description="ID로 특정 사용자의 상세 정보를 조회합니다.",
        responses={
            200: OpenApiResponse(UserSerializer, description="OK"),
            404: OpenApiResponse(description="Not Found"),
        },
        tags=["User"],
    ),
    create=extend_schema(
        summary="사용자 생성",
        description="새로운 사용자를 생성합니다.",
        request=UserSerializer,
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
)
class UserViewSet(viewsets.ModelViewSet):
    queryset = User.objects.all()
    serializer_class = UserSerializer
