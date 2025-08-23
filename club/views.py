from drf_spectacular.utils import OpenApiResponse, extend_schema, extend_schema_view
from rest_framework import status, viewsets
from rest_framework.decorators import action
from rest_framework.permissions import AllowAny
from rest_framework.response import Response

from .models import Club, ClubMember
from .serializers import ClubLoginRequestSerializer, ClubLoginResponseSerializer, ClubMemberSerializer, ClubSerializer


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
    login=extend_schema(
        summary="클럽 로그인",
        description="username을 입력하면 클럽 pk를 반환함",
        tags=["User"],
        request=ClubLoginRequestSerializer,
        responses={200: ClubLoginResponseSerializer},
    ),
)
class ClubMemberViewSet(viewsets.ModelViewSet):
    queryset = ClubMember.objects.all()
    serializer_class = ClubMemberSerializer

    @action(detail=False, methods=["post"], url_path="club-login", permission_classes=[AllowAny])
    def club_login(self, request):
        in_ser = ClubLoginRequestSerializer(data=request.data)
        if not in_ser.is_valid():
            return Response(in_ser.errors, status=status.HTTP_400_BAD_REQUEST)
        email = (in_ser.validated_data or {}).get("email")
        from user.models import User

        try:
            user = User.objects.get(email=email)
            club_member = ClubMember.objects.get(user=user.pk)
        except User.DoesNotExist:
            return Response({"detail": "해당 이메일의 사용자가 존재하지 않습니다."}, status=status.HTTP_404_NOT_FOUND)

        out_ser = ClubLoginResponseSerializer({"pk": club_member.pk})
        return Response(out_ser.data, status=status.HTTP_200_OK)
