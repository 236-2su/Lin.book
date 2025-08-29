import json

from django.shortcuts import get_object_or_404
from drf_spectacular.utils import OpenApiParameter, OpenApiResponse, extend_schema, extend_schema_view
from rest_framework import viewsets
from rest_framework.decorators import action, parser_classes
from rest_framework.parsers import MultiPartParser
from rest_framework.response import Response

from club.models import Club

from .models import Event, Ledger, LedgerTransactions, Receipt
from .serializers import (
    EventSerializer,
    EventTransactionSerializer,
    LedgerCreateSerializer,
    LedgerSerializer,
    LedgerTransactionCreateSerializer,
    LedgerTransactionsSerializer,
    ReceiptSerializer,
)
from .services import ocr_from_file
from .utils import sync_ledger_amount


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
            OpenApiParameter(name="pk", description="장부 ID", required=True, type=int, location=OpenApiParameter.PATH),
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
            OpenApiParameter(name="pk", description="장부 ID", required=True, type=int, location=OpenApiParameter.PATH),
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
            OpenApiParameter(name="pk", description="장부 ID", required=True, type=int, location=OpenApiParameter.PATH),
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
            OpenApiParameter(name="pk", description="장부 ID", required=True, type=int, location=OpenApiParameter.PATH),
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
                name="club_pk", description="클럽 ID", required=True, type=int, location=OpenApiParameter.PATH
            ),
        ],
        summary="클럽의 첫번째 장부의 거래 내역 목록 조회",
        description="클럽의 첫번째 장부에 속한 거래 내역 목록을 조회합니다.",
        responses={200: OpenApiResponse(response=LedgerTransactionsSerializer, description="OK")},
        tags=["LedgerTransactions"],
    ),
    retrieve=extend_schema(
        parameters=[
            OpenApiParameter(
                name="club_pk", description="클럽 ID", required=True, type=int, location=OpenApiParameter.PATH
            ),
            OpenApiParameter(
                name="ledger_pk", description="장부 ID", required=True, type=int, location=OpenApiParameter.PATH
            ),
            OpenApiParameter(
                name="pk", description="거래 내역 ID", required=True, type=int, location=OpenApiParameter.PATH
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
                name="club_pk", description="클럽 ID", required=True, type=int, location=OpenApiParameter.PATH
            ),
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
                name="club_pk", description="클럽 ID", required=True, type=int, location=OpenApiParameter.PATH
            ),
            OpenApiParameter(
                name="ledger_pk", description="장부 ID", required=True, type=int, location=OpenApiParameter.PATH
            ),
            OpenApiParameter(
                name="pk", description="거래 내역 ID", required=True, type=int, location=OpenApiParameter.PATH
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
                name="club_pk", description="클럽 ID", required=True, type=int, location=OpenApiParameter.PATH
            ),
            OpenApiParameter(
                name="ledger_pk", description="장부 ID", required=True, type=int, location=OpenApiParameter.PATH
            ),
            OpenApiParameter(
                name="pk", description="거래 내역 ID", required=True, type=int, location=OpenApiParameter.PATH
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
                name="club_pk", description="클럽 ID", required=True, type=int, location=OpenApiParameter.PATH
            ),
            OpenApiParameter(
                name="ledger_pk", description="장부 ID", required=True, type=int, location=OpenApiParameter.PATH
            ),
            OpenApiParameter(
                name="pk", description="거래 내역 ID", required=True, type=int, location=OpenApiParameter.PATH
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
        if self.action == "list":
            club_pk = self.kwargs.get("club_pk")
            ledger = Ledger.objects.filter(club_id=club_pk).first()
            if ledger:
                return LedgerTransactions.objects.filter(ledger_id=ledger.pk)
            return LedgerTransactions.objects.none()
        return LedgerTransactions.objects.filter(ledger_id=self.kwargs["ledger_pk"])

    def perform_create(self, serializer):
        ledger = get_object_or_404(Ledger, pk=self.kwargs["ledger_pk"])
        serializer.save(ledger=ledger)
        sync_ledger_amount(ledger)

    def perform_update(self, serializer):
        ledger = get_object_or_404(Ledger, pk=self.kwargs["ledger_pk"])
        serializer.save(ledger=ledger)
        sync_ledger_amount(ledger)

    def perform_destroy(self, instance):
        ledger = instance.ledger
        super().perform_destroy(instance)
        sync_ledger_amount(ledger)


@extend_schema_view(
    list=extend_schema(
        parameters=[
            OpenApiParameter(
                name="club_pk", description="클럽 ID", required=True, type=int, location=OpenApiParameter.PATH
            ),
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
                name="club_pk", description="클럽 ID", required=True, type=int, location=OpenApiParameter.PATH
            ),
            OpenApiParameter(
                name="ledger_pk", description="장부 ID", required=True, type=int, location=OpenApiParameter.PATH
            ),
            OpenApiParameter(
                name="pk", description="영수증 ID", required=True, type=int, location=OpenApiParameter.PATH
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
                name="club_pk", description="클럽 ID", required=True, type=int, location=OpenApiParameter.PATH
            ),
            OpenApiParameter(
                name="ledger_pk", description="장부 ID", required=True, type=int, location=OpenApiParameter.PATH
            ),
        ],
        summary="영수증 생성 및 OCR",
        description="새로운 영수증을 생성하고 OCR을 수행합니다. 요청 본문은 'multipart/form-data' 형식이어야 하며, 'image' 필드에 이미지 파일을 포함해야 합니다.",
        request={
            "multipart/form-data": {
                "type": "object",
                "properties": {"image": {"type": "string", "format": "binary"}},
            }
        },
        responses={
            201: OpenApiResponse(ReceiptSerializer, description="Created"),
            400: OpenApiResponse(description="Bad Request"),
        },
        tags=["Receipt"],
    ),
    update=extend_schema(
        parameters=[
            OpenApiParameter(
                name="club_pk", description="클럽 ID", required=True, type=int, location=OpenApiParameter.PATH
            ),
            OpenApiParameter(
                name="ledger_pk", description="장부 ID", required=True, type=int, location=OpenApiParameter.PATH
            ),
            OpenApiParameter(
                name="pk", description="영수증 ID", required=True, type=int, location=OpenApiParameter.PATH
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
                name="club_pk", description="클럽 ID", required=True, type=int, location=OpenApiParameter.PATH
            ),
            OpenApiParameter(
                name="ledger_pk", description="장부 ID", required=True, type=int, location=OpenApiParameter.PATH
            ),
            OpenApiParameter(
                name="pk", description="영수증 ID", required=True, type=int, location=OpenApiParameter.PATH
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
                name="club_pk", description="클럽 ID", required=True, type=int, location=OpenApiParameter.PATH
            ),
            OpenApiParameter(
                name="ledger_pk", description="장부 ID", required=True, type=int, location=OpenApiParameter.PATH
            ),
            OpenApiParameter(
                name="pk", description="영수증 ID", required=True, type=int, location=OpenApiParameter.PATH
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

    @parser_classes([MultiPartParser])
    def create(self, request, *args, **kwargs):
        image_file = request.FILES.get("image")
        if not image_file:
            return Response({"error": "Image file is required."}, status=400)

        ocr_result = ocr_from_file(image_file)
        if not ocr_result:
            return Response({"error": "OCR processing failed."}, status=500)

        processed_data = ocr_result.get("processed_data")
        if not processed_data:
            return Response({"error": "Failed to get processed data from OCR result."}, status=500)

        amount = processed_data.get("amount")
        date_time_str = processed_data.get("date_time")
        items = processed_data.get("details")
        vendor = processed_data.get("vendor", "알 수 없음")  # 가게 이름이 없으면 기본값 설정

        # 빈 문자열도 체크하도록 수정하고, 디버깅을 위해 OCR 결과도 응답에 포함
        if amount is None or not date_time_str or items is None:
            return Response(
                {
                    "error": "OCR result is missing required fields (amount, date_time, details).",
                    "ocr_data": processed_data,
                },
                status=400,
            )

        # 1. Receipt 인스턴스 생성
        receipt = Receipt.objects.create(image=image_file, amount=amount, date_time=date_time_str, items=items)

        # 2. 연관된 LedgerTransactions 레코드 생성
        ledger_pk = self.kwargs.get("ledger_pk")
        ledger = get_object_or_404(Ledger, pk=ledger_pk)

        # 구매 항목으로 설명 생성
        items_description = ", ".join([f"{item}: {price}원" for item, price in items.items()])
        description = f"{vendor} - {items_description}"

        LedgerTransactions.objects.create(
            ledger=ledger,
            date_time=receipt.date_time,
            amount=receipt.amount,
            payment_method="카드",  # OCR로 알 수 없으므로 '카드'를 기본값으로 가정
            receipt=receipt,
            description=description,
            vendor=vendor,
        )

        # 3. 장부 총액 동기화
        sync_ledger_amount(ledger)

        serializer = self.get_serializer(receipt)
        return Response(serializer.data, status=201)


@extend_schema_view(
    list=extend_schema(
        parameters=[
            OpenApiParameter(
                name="club_pk", description="클럽 ID", required=True, type=int, location=OpenApiParameter.PATH
            ),
        ],
        summary="이벤트 목록 조회",
        description="클럽의 전체 이벤트 목록을 조회합니다.",
        responses={200: OpenApiResponse(response=EventSerializer, description="OK")},
        tags=["Event"],
    ),
    retrieve=extend_schema(
        parameters=[
            OpenApiParameter(
                name="club_pk", description="클럽 ID", required=True, type=int, location=OpenApiParameter.PATH
            ),
            OpenApiParameter(
                name="event_pk", description="이벤트 ID", required=True, type=int, location=OpenApiParameter.PATH
            ),
        ],
        summary="특정 이벤트 조회",
        description="ID로 특정 이벤트의 상세 정보를 조회합니다.",
        responses={
            200: OpenApiResponse(EventSerializer, description="OK"),
            404: OpenApiResponse(description="Not Found"),
        },
        tags=["Event"],
    ),
    create=extend_schema(
        parameters=[
            OpenApiParameter(
                name="club_pk", description="클럽 ID", required=True, type=int, location=OpenApiParameter.PATH
            ),
        ],
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
        parameters=[
            OpenApiParameter(
                name="club_pk", description="클럽 ID", required=True, type=int, location=OpenApiParameter.PATH
            ),
            OpenApiParameter(
                name="event_pk", description="이벤트 ID", required=True, type=int, location=OpenApiParameter.PATH
            ),
        ],
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
        parameters=[
            OpenApiParameter(
                name="club_pk", description="클럽 ID", required=True, type=int, location=OpenApiParameter.PATH
            ),
            OpenApiParameter(
                name="event_pk", description="이벤트 ID", required=True, type=int, location=OpenApiParameter.PATH
            ),
        ],
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
        parameters=[
            OpenApiParameter(
                name="club_pk", description="클럽 ID", required=True, type=int, location=OpenApiParameter.PATH
            ),
            OpenApiParameter(
                name="event_pk", description="이벤트 ID", required=True, type=int, location=OpenApiParameter.PATH
            ),
        ],
        summary="이벤트 삭제",
        description="ID로 특정 이벤트를 삭제합니다.",
        responses={204: OpenApiResponse(description="No Content"), 404: OpenApiResponse(description="Not Found")},
        tags=["Event"],
    ),
)
class EventViewSet(viewsets.ModelViewSet):
    serializer_class = EventSerializer
    lookup_url_kwarg = "event_pk"

    def get_queryset(self):
        return Event.objects.filter(club_id=self.kwargs["club_pk"])

    def perform_create(self, serializer):
        club = get_object_or_404(Club, pk=self.kwargs["club_pk"])
        serializer.save(club=club)

    @extend_schema(
        summary="이벤트 거래 내역 조회",
        description="특정 이벤트에 대한 모든 거래 내역을 조회합니다.",
        responses={200: LedgerTransactionsSerializer(many=True)},
        tags=["Event"],
    )
    @action(detail=True, methods=["get"], url_path="transactions")
    def get_transaction(self, request, *args, **kwargs):
        event = self.get_object()
        transactions = LedgerTransactions.objects.filter(event=event)
        serializer = LedgerTransactionsSerializer(transactions, many=True)
        return Response(serializer.data)


@extend_schema_view(
    list=extend_schema(
        summary="이벤트별 거래 내역 요약 조회",
        description="각 이벤트별로 거래 내역의 총합을 요약하여 조회합니다.",
        responses={200: EventTransactionSerializer(many=True)},
        tags=["Event"],
    )
)
class EventTransactionSummaryViewSet(viewsets.ReadOnlyModelViewSet):
    serializer_class = EventTransactionSerializer

    def get_queryset(self):
        club_pk = self.kwargs.get("club_pk")
        return Event.objects.filter(club_id=club_pk).prefetch_related("ledgertransactions_set")
