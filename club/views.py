from django.db import transaction
from django.shortcuts import get_object_or_404
from drf_spectacular.utils import OpenApiParameter, OpenApiResponse, extend_schema, extend_schema_view
from rest_framework import status, viewsets
from rest_framework.decorators import action
from rest_framework.parsers import FormParser, MultiPartParser
from rest_framework.response import Response
from rest_framework.views import APIView

from ledger.models import Event
from ledger.serializers import EventSerializer
from user.models import User

from .models import Club, ClubMember, ClubWelcomePage
from .serializers import (
    ClubCreateSerializer,
    ClubLoginRequestSerializer,
    ClubLoginResponseSerializer,
    ClubMemberCreateSerializer,
    ClubMemberSerializer,
    ClubSerializer,
    ClubWelcomePageSerializer,
)
from .services import similar_by_club, similar_by_text


@extend_schema_view(
    get=extend_schema(
        operation_id="club_similar_by_query_retrieve",
        summary="검색 추천",
        description="검색어를 넣으면 유사한 동아리를 추천해 줍니다",
        tags=["Club"],
        parameters=[
            OpenApiParameter(
                name="query", description="Search query for club name, description, tags", required=True, type=str
            ),
            OpenApiParameter(name="major", description="Filter by major category", required=False, type=str),
            OpenApiParameter(
                name="min_members", description="Filter by minimum number of members", required=False, type=int
            ),
        ],
    )
)
class SimilarClubsByQuery(APIView):
    serializer_class = ClubSerializer

    def get(self, request):
        query = request.query_params.get("query")
        major = request.query_params.get("major")
        min_members = request.query_params.get("min_members")
        if not query:
            return Response({"detail": "query is required"}, status=400)
        filters = {}
        if major:
            filters["major"] = major
        if min_members:
            filters["min_members"] = int(min_members)
        data = similar_by_text(query, k=10, filters=filters)
        return Response({"results": data})


@extend_schema_view(
    get=extend_schema(
        operation_id="club_similar_by_id_retrieve",
        summary="id 추천",
        description="가입한 동아리를 기반으로 다른 동아리를 추천합니다",
        tags=["Club"],
    )
)
class SimilarClubsById(APIView):
    serializer_class = ClubSerializer

    def get(self, request, club_id):
        data = similar_by_club(club_id, k=10)
        return Response({"results": data})


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
    parser_classes = (MultiPartParser, FormParser)

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
        description="email을 입력하면 해당 동아리의 멤버 pk를 반환함",
        tags=["ClubMember"],
        request=ClubLoginRequestSerializer,
        responses={200: ClubLoginResponseSerializer},
    ),
)
class ClubMemberViewSet(viewsets.ModelViewSet):
    serializer_class = ClubMemberSerializer

    def get_queryset(self):
        return ClubMember.objects.filter(club_id=self.kwargs["club_pk"])

    def perform_create(self, serializer):
        club = get_object_or_404(Club, pk=self.kwargs["club_pk"])
        serializer.save(club=club)

    @action(detail=False, methods=["post"])
    def login(self, request, club_pk=None):
        serializer = ClubLoginRequestSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        email = serializer.validated_data["email"]

        try:
            club_member = ClubMember.objects.get(club_id=club_pk, user__email=email)
            response_serializer = ClubLoginResponseSerializer({"pk": club_member.pk})
            return Response(response_serializer.data, status=status.HTTP_200_OK)
        except ClubMember.DoesNotExist:
            return Response({"detail": "Member not found in this club."}, status=status.HTTP_404_NOT_FOUND)

    @extend_schema(
        summary="가입 대기 중인 클럽 멤버 목록 조회",
        description="가입 대기 중인 클럽 멤버 목록을 조회합니다.",
        responses={200: ClubMemberSerializer(many=True)},
        tags=["ClubMember"],
    )
    @action(detail=False, methods=["get"])
    def waiting(self, request, club_pk=None):
        waiting_members = self.get_queryset().filter(status="waiting")
        serializer = self.get_serializer(waiting_members, many=True)
        return Response(serializer.data)


@extend_schema_view(
    list=extend_schema(
        summary="클럽 Welcome Page 목록 조회",
        description="특정 클럽의 Welcome Page를 조회합니다. (OneToOne 관계이므로 1개만 반환)",
        tags=["ClubWelcomePage"],
    ),
    retrieve=extend_schema(
        summary="클럽 Welcome Page 상세 조회",
        description="ID로 특정 클럽 Welcome Page의 상세 정보를 조회합니다.",
        tags=["ClubWelcomePage"],
    ),
    create=extend_schema(
        summary="클럽 Welcome Page 생성",
        description="특정 클럽에 대한 Welcome Page를 생성합니다.",
        tags=["ClubWelcomePage"],
    ),
    update=extend_schema(
        summary="클럽 Welcome Page 전체 수정 (PUT)",
        description="클럽 Welcome Page의 모든 필드를 갱신합니다.",
        tags=["ClubWelcomePage"],
    ),
    partial_update=extend_schema(
        summary="클럽 Welcome Page 부분 수정 (PATCH)",
        description="클럽 Welcome Page의 일부 필드만 부분 갱신합니다.",
        tags=["ClubWelcomePage"],
    ),
    destroy=extend_schema(
        summary="클럽 Welcome Page 삭제",
        description="ID로 특정 클럽 Welcome Page를 삭제합니다.",
        tags=["ClubWelcomePage"],
    ),
)
class ClubWelcomePageViewSet(viewsets.ModelViewSet):
    queryset = ClubWelcomePage.objects.all()
    serializer_class = ClubWelcomePageSerializer
    parser_classes = (MultiPartParser, FormParser)

    def get_queryset(self):
        queryset = super().get_queryset()
        club_pk = self.kwargs.get("club_pk")
        if club_pk:
            queryset = queryset.filter(club__pk=club_pk)
        return queryset

    def perform_create(self, serializer):
        from rest_framework.exceptions import ValidationError

        club_pk = self.kwargs.get("club_pk")
        club = Club.objects.get(pk=club_pk)
        if ClubWelcomePage.objects.filter(club=club).exists():
            raise ValidationError({"detail": "해당 클럽의 Welcome Page가 이미 존재합니다."})
        serializer.save(club=club)


@extend_schema_view(
    list=extend_schema(
        summary="이벤트 목록 조회",
        description="클럽의 전체 이벤트 목록을 조회합니다.",
        responses={200: OpenApiResponse(response=EventSerializer, description="OK")},
        tags=["Event"],
    ),
    retrieve=extend_schema(
        summary="특정 이벤트 조회",
        description="ID로 특정 이벤트의 상세 정보를 조회합니다.",
        responses={
            200: OpenApiResponse(EventSerializer, description="OK"),
            404: OpenApiResponse(description="Not Found"),
        },
        tags=["Event"],
    ),
    create=extend_schema(
        summary="이벤트 생성",
        description="새로운 이벤트를 생성합니다.",
        request=EventSerializer,
        responses={
            201: OpenApiResponse(EventSerializer, description="Created"),
            400: OpenApiResponse(description="Bad Request"),
        },
        tags=["Event"],
    ),
    update=extend_schema(
        summary="이벤트 정보 전체 수정 (PUT)",
        description="이벤트의 모든 필드를 갱신합니다.",
        request=EventSerializer,
        responses={
            200: OpenApiResponse(EventSerializer, description="OK"),
            400: OpenApiResponse(description="Bad Request"),
            404: OpenApiResponse(description="Not Found"),
        },
        tags=["Event"],
    ),
    partial_update=extend_schema(
        summary="이벤트 정보 부분 수정 (PATCH)",
        description="이벤트의 일부 필드만 부분 갱신합니다.",
        request=EventSerializer,
        responses={
            200: OpenApiResponse(EventSerializer, description="OK"),
            400: OpenApiResponse(description="Bad Request"),
            404: OpenApiResponse(description="Not Found"),
        },
        tags=["Event"],
    ),
    destroy=extend_schema(
        summary="이벤트 삭제",
        description="ID로 특정 이벤트를 삭제합니다.",
        responses={204: OpenApiResponse(description="No Content"), 404: OpenApiResponse(description="Not Found")},
        tags=["Event"],
    ),
)
class EventViewSet(viewsets.ModelViewSet):
    serializer_class = EventSerializer

    def get_queryset(self):
        return Event.objects.filter(club_id=self.kwargs["club_pk"])

    def perform_create(self, serializer):
        club = get_object_or_404(Club, pk=self.kwargs["club_pk"])
        serializer.save(club=club)
