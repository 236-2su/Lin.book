from drf_spectacular.utils import OpenApiResponse, extend_schema, extend_schema_view
from rest_framework import viewsets

from .models import Club, User
from .serializers import ClubMemberSerializer, ClubSerializer


@extend_schema_view(
    list=extend_schema(
        summary="클럽 목록 조회",
        description="전체 클럽 목록을 조회합니다.",
        responses={200: OpenApiResponse(response=ClubSerializer, description="OK")},
        tags=["Club"],
    ),
    retrieve=extend_schema(
        summary="특정 클럽 조회",
        description="ID로 특정 클럽의 상세 정보를 조회합니다.",
        responses={
            200: OpenApiResponse(ClubSerializer, description="OK"),
            404: OpenApiResponse(description="Not Found"),
        },
        tags=["Club"],
    ),
    create=extend_schema(
        summary="클럽 생성",
        description="새로운 클럽을 생성합니다.",
        request=ClubSerializer,
        responses={
            201: OpenApiResponse(ClubSerializer, description="Created"),
            400: OpenApiResponse(description="Bad Request"),
        },
        tags=["Club"],
    ),
    update=extend_schema(
        summary="클럽 정보 전체 수정 (PUT)",
        description="클럽의 모든 필드를 갱신합니다.",
        request=ClubSerializer,
        responses={
            200: OpenApiResponse(ClubSerializer, description="OK"),
            400: OpenApiResponse(description="Bad Request"),
            404: OpenApiResponse(description="Not Found"),
        },
        tags=["Club"],
    ),
    partial_update=extend_schema(
        summary="클럽 정보 부분 수정 (PATCH)",
        description="클럽의 일부 필드만 부분 갱신합니다.",
        request=ClubSerializer,
        responses={
            200: OpenApiResponse(ClubSerializer, description="OK"),
            400: OpenApiResponse(description="Bad Request"),
            404: OpenApiResponse(description="Not Found"),
        },
        tags=["Club"],
    ),
    destroy=extend_schema(
        summary="클럽 삭제",
        description="ID로 특정 클럽을 삭제합니다.",
        responses={204: OpenApiResponse(description="No Content"), 404: OpenApiResponse(description="Not Found")},
        tags=["Club"],
    ),
)
class ClubViewSet(viewsets.ModelViewSet):
    queryset = Club.objects.all()
    serializer_class = ClubSerializer


@extend_schema_view(
    list=extend_schema(
        summary="클럽 멤버 목록 조회",
        description="전체 클럽 멤버 목록을 조회합니다.",
        responses={200: OpenApiResponse(response=ClubMemberSerializer, description="OK")},
        tags=["ClubMember"],
    ),
    retrieve=extend_schema(
        summary="특정 클럽 멤버 조회",
        description="ID로 특정 클럽 멤버의 상세 정보를 조회합니다.",
        responses={
            200: OpenApiResponse(ClubMemberSerializer, description="OK"),
            404: OpenApiResponse(description="Not Found"),
        },
        tags=["ClubMember"],
    ),
    create=extend_schema(
        summary="클럽 멤버 추가",
        description="새로운 클럽 멤버를 추가합니다.",
        request=ClubMemberSerializer,
        responses={
            201: OpenApiResponse(ClubMemberSerializer, description="Created"),
            400: OpenApiResponse(description="Bad Request"),
        },
        tags=["ClubMember"],
    ),
    update=extend_schema(
        summary="클럽 멤버 정보 전체 수정 (PUT)",
        description="클럽 멤버의 모든 필드를 갱신합니다.",
        request=ClubMemberSerializer,
        responses={
            200: OpenApiResponse(ClubMemberSerializer, description="OK"),
            400: OpenApiResponse(description="Bad Request"),
            404: OpenApiResponse(description="Not Found"),
        },
        tags=["ClubMember"],
    ),
    partial_update=extend_schema(
        summary="클럽 멤버 정보 부분 수정 (PATCH)",
        description="클럽 멤버의 일부 필드만 부분 갱신합니다.",
        request=ClubMemberSerializer,
        responses={
            200: OpenApiResponse(ClubMemberSerializer, description="OK"),
            400: OpenApiResponse(description="Bad Request"),
            404: OpenApiResponse(description="Not Found"),
        },
        tags=["ClubMember"],
    ),
    destroy=extend_schema(
        summary="클럽 멤버 삭제",
        description="ID로 특정 클럽 멤버를 삭제합니다.",
        responses={204: OpenApiResponse(description="No Content"), 404: OpenApiResponse(description="Not Found")},
        tags=["ClubMember"],
    ),
)
class ClubMemberViewSet(viewsets.ModelViewSet):
    queryset = User.objects.all()
    serializer_class = ClubMemberSerializer
