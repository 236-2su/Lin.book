from django.shortcuts import get_object_or_404
from drf_spectacular.utils import OpenApiParameter, OpenApiResponse, extend_schema, extend_schema_view
from rest_framework import status, viewsets
from rest_framework.response import Response

from club.models import Club

from .models import Ledger, LedgerTransactions, Receipt
from .serializers import (
    LedgerCreateSerializer,
    LedgerSerializer,
    LedgerTransactionCreateSerializer,
    LedgerTransactionsSerializer,
    ReceiptSerializer,
)


@extend_schema_view(
    list=extend_schema(
        parameters=[
            OpenApiParameter(
                name="club_pk", description="클럽 ID", required=True, type=int, location=OpenApiParameter.PATH
            ),
        ],
        summary="장부 목록 조회",
        description="전체 장부 목록을 조회합니다.",
        responses={200: OpenApiResponse(response=LedgerSerializer, description="OK")},
        tags=["Ledger"],
    ),
    retrieve=extend_schema(
        parameters=[
            OpenApiParameter(
                name="club_pk", description="클럽 ID", required=True, type=int, location=OpenApiParameter.PATH
            ),
        ],
        summary="특정 장부 조회",
        description="ID로 특정 장부의 상세 정보를 조회합니다.",
        responses={
            200: OpenApiResponse(LedgerSerializer, description="OK"),
            404: OpenApiResponse(description="Not Found"),
        },
        tags=["Ledger"],
    ),
    create=extend_schema(
        parameters=[
            OpenApiParameter(
                name="club_pk", description="클럽 ID", required=True, type=int, location=OpenApiParameter.PATH
            ),
        ],
        summary="장부 생성",
        description="새로운 장부를 생성합니다.",
        request=LedgerCreateSerializer,
        responses={
            201: OpenApiResponse(LedgerSerializer, description="Created"),
            400: OpenApiResponse(description="Bad Request"),
        },
        tags=["Ledger"],
    ),
    update=extend_schema(
        parameters=[
            OpenApiParameter(
                name="club_pk", description="클럽 ID", required=True, type=int, location=OpenApiParameter.PATH
            ),
        ],
        summary="장부 정보 전체 수정 (PUT)",
        description="장부의 모든 필드를 갱신합니다.",
        request=LedgerSerializer,
        responses={
            200: OpenApiResponse(LedgerSerializer, description="OK"),
            400: OpenApiResponse(description="Bad Request"),
            404: OpenApiResponse(description="Not Found"),
        },
        tags=["Ledger"],
    ),
    partial_update=extend_schema(
        parameters=[
            OpenApiParameter(
                name="club_pk", description="클럽 ID", required=True, type=int, location=OpenApiParameter.PATH
            ),
        ],
        summary="장부 정보 부분 수정 (PATCH)",
        description="장부의 일부 필드만 부분 갱신합니다.",
        request=LedgerSerializer,
        responses={
            200: OpenApiResponse(LedgerSerializer, description="OK"),
            400: OpenApiResponse(description="Bad Request"),
            404: OpenApiResponse(description="Not Found"),
        },
        tags=["Ledger"],
    ),
    destroy=extend_schema(
        parameters=[
            OpenApiParameter(
                name="club_pk", description="클럽 ID", required=True, type=int, location=OpenApiParameter.PATH
            ),
        ],
        summary="장부 삭제",
        description="ID로 특정 장부를 삭제합니다.",
        responses={204: OpenApiResponse(description="No Content"), 404: OpenApiResponse(description="Not Found")},
        tags=["Ledger"],
    ),
)
class LedgerViewSet(viewsets.ModelViewSet):
    serializer_class = LedgerSerializer

    def get_queryset(self):
        return Ledger.objects.filter(club_id=self.kwargs["club_pk"])

    def perform_create(self, serializer):
        club = get_object_or_404(Club, pk=self.kwargs["club_pk"])
        serializer.save(club=club)


@extend_schema_view(
    list=extend_schema(
        parameters=[
            OpenApiParameter(
                name="ledger_pk", description="장부 ID", required=True, type=int, location=OpenApiParameter.PATH
            ),
        ],
        summary="특정 장부의 거래 내역 목록 조회",
        description="특정 장부에 속한 거래 내역 목록을 조회합니다.",
        responses={200: OpenApiResponse(response=LedgerTransactionsSerializer, description="OK")},
        tags=["LedgerTransactions"],
    ),
    retrieve=extend_schema(
        parameters=[
            OpenApiParameter(
                name="ledger_pk", description="장부 ID", required=True, type=int, location=OpenApiParameter.PATH
            ),
        ],
        summary="특정 거래 내역 조회",
        description="ID로 특정 거래 내역의 상세 정보를 조회합니다.",
        responses={
            200: OpenApiResponse(LedgerTransactionsSerializer, description="OK"),
            404: OpenApiResponse(description="Not Found"),
        },
        tags=["LedgerTransactions"],
    ),
    create=extend_schema(
        parameters=[
            OpenApiParameter(
                name="ledger_pk", description="장부 ID", required=True, type=int, location=OpenApiParameter.PATH
            ),
        ],
        summary="특정 장부에 거래 내역 생성",
        description="특정 장부에 새로운 거래 내역을 생성합니다.",
        request=LedgerTransactionCreateSerializer,
        responses={
            201: OpenApiResponse(LedgerTransactionsSerializer, description="Created"),
            400: OpenApiResponse(description="Bad Request"),
        },
        tags=["LedgerTransactions"],
    ),
    update=extend_schema(
        parameters=[
            OpenApiParameter(
                name="ledger_pk", description="장부 ID", required=True, type=int, location=OpenApiParameter.PATH
            ),
        ],
        summary="거래 내역 전체 수정 (PUT)",
        description="거래 내역의 모든 필드를 갱신합니다.",
        request=LedgerTransactionsSerializer,
        responses={
            200: OpenApiResponse(LedgerTransactionsSerializer, description="OK"),
            400: OpenApiResponse(description="Bad Request"),
            404: OpenApiResponse(description="Not Found"),
        },
        tags=["LedgerTransactions"],
    ),
    partial_update=extend_schema(
        parameters=[
            OpenApiParameter(
                name="ledger_pk", description="장부 ID", required=True, type=int, location=OpenApiParameter.PATH
            ),
        ],
        summary="거래 내역 부분 수정 (PATCH)",
        description="거래 내역의 일부 필드만 부분 갱신합니다.",
        request=LedgerTransactionsSerializer,
        responses={
            200: OpenApiResponse(LedgerTransactionsSerializer, description="OK"),
            400: OpenApiResponse(description="Bad Request"),
            404: OpenApiResponse(description="Not Found"),
        },
        tags=["LedgerTransactions"],
    ),
    destroy=extend_schema(
        parameters=[
            OpenApiParameter(
                name="ledger_pk", description="장부 ID", required=True, type=int, location=OpenApiParameter.PATH
            ),
        ],
        summary="거래 내역 삭제",
        description="ID로 특정 거래 내역을 삭제합니다.",
        responses={204: OpenApiResponse(description="No Content"), 404: OpenApiResponse(description="Not Found")},
        tags=["LedgerTransactions"],
    ),
)
class LedgerTransactionsViewSet(viewsets.ModelViewSet):
    serializer_class = LedgerTransactionsSerializer

    def get_queryset(self):
        return LedgerTransactions.objects.filter(ledger_id=self.kwargs["ledger_pk"])

    def perform_create(self, serializer):
        ledger = get_object_or_404(Ledger, pk=self.kwargs["ledger_pk"])
        serializer.save(ledger=ledger)


@extend_schema_view(
    list=extend_schema(
        parameters=[
            OpenApiParameter(
                name="ledger_pk", description="장부 ID", required=True, type=int, location=OpenApiParameter.PATH
            ),
        ],
        summary="특정 장부의 영수증 목록 조회",
        description="특정 장부에 속한 모든 거래 내역의 영수증 목록을 조회합니다.",
        responses={200: OpenApiResponse(response=ReceiptSerializer, description="OK")},
        tags=["Receipt"],
    ),
    retrieve=extend_schema(
        parameters=[
            OpenApiParameter(
                name="ledger_pk", description="장부 ID", required=True, type=int, location=OpenApiParameter.PATH
            ),
        ],
        summary="특정 영수증 조회",
        description="ID로 특정 영수증의 상세 정보를 조회합니다.",
        responses={
            200: OpenApiResponse(ReceiptSerializer, description="OK"),
            404: OpenApiResponse(description="Not Found"),
        },
        tags=["Receipt"],
    ),
    create=extend_schema(
        parameters=[
            OpenApiParameter(
                name="ledger_pk", description="장부 ID", required=True, type=int, location=OpenApiParameter.PATH
            ),
        ],
        summary="영수증 생성",
        description="새로운 영수증을 생성합니다.",
        request=ReceiptSerializer,
        responses={
            201: OpenApiResponse(ReceiptSerializer, description="Created"),
            400: OpenApiResponse(description="Bad Request"),
        },
        tags=["Receipt"],
    ),
    update=extend_schema(
        parameters=[
            OpenApiParameter(
                name="ledger_pk", description="장부 ID", required=True, type=int, location=OpenApiParameter.PATH
            ),
        ],
        summary="영수증 정보 전체 수정 (PUT)",
        description="영수증의 모든 필드를 갱신합니다.",
        request=ReceiptSerializer,
        responses={
            200: OpenApiResponse(ReceiptSerializer, description="OK"),
            400: OpenApiResponse(description="Bad Request"),
            404: OpenApiResponse(description="Not Found"),
        },
        tags=["Receipt"],
    ),
    partial_update=extend_schema(
        parameters=[
            OpenApiParameter(
                name="ledger_pk", description="장부 ID", required=True, type=int, location=OpenApiParameter.PATH
            ),
        ],
        summary="영수증 정보 부분 수정 (PATCH)",
        description="영수증의 일부 필드만 부분 갱신합니다.",
        request=ReceiptSerializer,
        responses={
            200: OpenApiResponse(ReceiptSerializer, description="OK"),
            400: OpenApiResponse(description="Bad Request"),
            404: OpenApiResponse(description="Not Found"),
        },
        tags=["Receipt"],
    ),
    destroy=extend_schema(
        parameters=[
            OpenApiParameter(
                name="ledger_pk", description="장부 ID", required=True, type=int, location=OpenApiParameter.PATH
            ),
        ],
        summary="영수증 삭제",
        description="ID로 특정 영수증을 삭제합니다.",
        responses={204: OpenApiResponse(description="No Content"), 404: OpenApiResponse(description="Not Found")},
        tags=["Receipt"],
    ),
)
class ReceiptViewSet(viewsets.ModelViewSet):
    serializer_class = ReceiptSerializer

    def get_queryset(self):
        return Receipt.objects.filter(ledgertransactions__ledger_id=self.kwargs["ledger_pk"])
