from django.db import transaction
from drf_spectacular.utils import OpenApiResponse, extend_schema, extend_schema_view
from rest_framework import status, viewsets
from rest_framework.decorators import action
from rest_framework.permissions import AllowAny
from rest_framework.response import Response

from user.models import User

from .models import Club, ClubMember
from .serializers import (
    ClubCreateSerializer,
    ClubLoginRequestSerializer,
    ClubLoginResponseSerializer,
    ClubMemberCreateSerializer,
    ClubMemberSerializer,
    ClubSerializer,
)


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
        description="""새로운 클럽을 생성합니다. 요청 `body`에 `admin` 필드로 `user`의 `pk`를 포함해야 합니다.

ex) `{"name": "test club", ..., "admin": 1}`""",
        request=ClubCreateSerializer,
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

    def create(self, request, *args, **kwargs):
        data = request.data.copy()
        admin_pk = data.pop("admin", None)

        if admin_pk is None:
            return Response({"admin": ["This field is required."]}, status=status.HTTP_400_BAD_REQUEST)

        try:
            admin_user = User.objects.get(pk=admin_pk)
        except User.DoesNotExist:
            return Response({"admin": [f"User with pk {admin_pk} does not exist."]}, status=status.HTTP_400_BAD_REQUEST)

        serializer = self.get_serializer(data=data)
        serializer.is_valid(raise_exception=True)

        with transaction.atomic():
            club = serializer.save()
            ClubMember.objects.create(
                club=club, user=admin_user, role="leader", status="active", amount_fee=0, paid_fee=0
            )

        headers = self.get_success_headers(serializer.data)
        return Response(serializer.data, status=status.HTTP_201_CREATED, headers=headers)


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
        request=ClubMemberCreateSerializer,
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

    def perform_create(self, serializer):
        from rest_framework.exceptions import ValidationError

        if ClubMember.objects.filter(
            user=serializer.validated_data["user"], club=serializer.validated_data["club"]
        ).exists():
            raise ValidationError({"detail": "이미 해당 클럽에 가입된 유저입니다."})
        serializer.save()

    def get_queryset(self):
        queryset = super().get_queryset()
        club_pk = self.kwargs.get("club_pk")
        if club_pk:
            queryset = queryset.filter(club__pk=club_pk)
        return queryset

    @action(detail=False, methods=["post"], url_path="club-login", permission_classes=[AllowAny])
    def club_login(self, request):
        in_ser = ClubLoginRequestSerializer(data=request.data)
        if not in_ser.is_valid():
            return Response(in_ser.errors, status=status.HTTP_400_BAD_REQUEST)
        email = (in_ser.validated_data or {}).get("email")
        club_id = (in_ser.validated_data or {}).get("club_id")
        from user.models import User

        try:
            user = User.objects.get(email=email)
            club_member = ClubMember.objects.get(user=user.pk, club_id=club_id)
        except User.DoesNotExist:
            return Response({"detail": "해당 이메일의 사용자가 존재하지 않습니다."}, status=status.HTTP_404_NOT_FOUND)
        except ClubMember.DoesNotExist:
            return Response({"detail": "해당 클럽에 가입된 사용자가 아닙니다."}, status=status.HTTP_404_NOT_FOUND)

        out_ser = ClubLoginResponseSerializer({"pk": club_member.pk})
        return Response(out_ser.data, status=status.HTTP_200_OK)
