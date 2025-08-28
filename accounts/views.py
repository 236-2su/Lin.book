from django.shortcuts import get_object_or_404
from drf_spectacular.utils import OpenApiParameter, OpenApiResponse, extend_schema, extend_schema_view
from rest_framework import status, viewsets
from rest_framework.decorators import action
from rest_framework.exceptions import ValidationError
from rest_framework.response import Response

from club.models import Club
from user.models import User

from .models import Accounts
from .serializers import AccountCreateRequestSerializer, AccountInternalSerializer, AccountResponseSerializer
from .services import create_account, get_account_balance, get_transaction_history


@extend_schema_view(
    list=extend_schema(
        summary="클럽의 계좌 목록 조회",
        responses={200: AccountResponseSerializer(many=True)},
        tags=["Accounts"],
    ),
    retrieve=extend_schema(
        summary="클럽의 특정 계좌 상세 조회",
        responses={200: AccountResponseSerializer},
        tags=["Accounts"],
    ),
    create=extend_schema(
        summary="클럽 계좌 생성",
        request=AccountCreateRequestSerializer,
        responses={201: AccountResponseSerializer},
        tags=["Accounts"],
    ),
    destroy=extend_schema(
        summary="클럽 계좌 삭제",
        responses={204: OpenApiResponse(description="No Content")},
        tags=["Accounts"],
    ),
)
class ClubAccountsViewSet(viewsets.ModelViewSet):
    queryset = Accounts.objects.select_related("user", "club")
    serializer_class = AccountInternalSerializer
    lookup_url_kwarg = "account_pk"

    def get_queryset(self):
        return super().get_queryset().filter(club_id=self.kwargs["club_pk"])

    def perform_create(self, serializer):
        club = get_object_or_404(Club, pk=self.kwargs["club_pk"])
        user_id = self.request.data.get("user_id")
        if not user_id:
            raise ValidationError("user_id is required.")
        user = get_object_or_404(User, pk=user_id)

        try:
            account_no = create_account(user)
        except ValidationError as e:
            raise e

        serializer.save(user=user, club=club, code=account_no, amount=0)

    @extend_schema(
        summary="계좌 잔액 조회",
        description="특정 계좌의 잔액을 외부 API를 통해 조회합니다.",
        responses={200: OpenApiResponse(description="External API response body")},
        tags=["Accounts"],
    )
    @action(detail=True, methods=["get"])
    def balance(self, request, club_pk=None, account_pk=None):
        account = self.get_object()
        try:
            balance_data = get_account_balance(account.user, account.code)
            return Response(balance_data)
        except ValidationError as e:
            return Response(e.detail, status=status.HTTP_400_BAD_REQUEST)

    @extend_schema(
        summary="계좌 거래 내역 조회",
        description="특정 계좌의 거래 내역을 외부 API를 통해 조회합니다. 날짜를 미입력시 기본으로 최근 1년의 데이터를 조회합니다.",
        parameters=[
            OpenApiParameter(name="startDate", description="조회시작일자 (YYYYMMDD)", required=False, type=str),
            OpenApiParameter(name="endDate", description="조회종료일자 (YYYYMMDD)", required=False, type=str),
            OpenApiParameter(
                name="transactionType",
                description="거래구분 (M:입금, D:출금, A:전체), 미입력시 기본값 A",
                required=False,
                type=str,
            ),
            OpenApiParameter(
                name="orderByType",
                description="정렬순서 (ASC:오름차순, DESC:내림차순), 미입력시 기본값 DESC",
                required=False,
                type=str,
            ),
        ],
        responses={200: OpenApiResponse(description="External API response body")},
        tags=["Accounts"],
    )
    @action(detail=True, methods=["get"])
    def history(self, request, club_pk=None, account_pk=None):
        account = self.get_object()

        start_date = request.query_params.get("startDate")
        end_date = request.query_params.get("endDate")
        transaction_type = request.query_params.get("transactionType", "A")
        order_by = request.query_params.get("orderByType", "DESC")

        try:
            history_data = get_transaction_history(
                user=account.user,
                account_no=account.code,
                start_date=start_date,
                end_date=end_date,
                transaction_type=transaction_type,
                order_by=order_by,
            )
            return Response(history_data)
        except ValidationError as e:
            return Response(e.detail, status=status.HTTP_400_BAD_REQUEST)
