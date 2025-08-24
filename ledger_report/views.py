from django.shortcuts import get_object_or_404
from drf_spectacular.utils import OpenApiParameter, OpenApiResponse, extend_schema
from rest_framework import status
from rest_framework.response import Response
from rest_framework.views import APIView

from ledger.models import Ledger

from .serializers import MonthlyLedgerStatsResponseSerializer
from .services import monthly_ledger_stats


class MonthlyLedgerReportView(APIView):
    @extend_schema(
        summary="월간 장부 리포트",
        description="해당 장부의 특정 월 통계를 반환합니다. (수입=양수, 지출=음수 가정)",
        parameters=[
            OpenApiParameter(
                name="club_pk", description="클럽 ID", required=True, type=int, location=OpenApiParameter.PATH
            ),
            OpenApiParameter(
                name="ledger_pk", description="장부 ID", required=True, type=int, location=OpenApiParameter.PATH
            ),
            OpenApiParameter(
                name="year", description="년도(예: 2025)", required=False, type=int, location=OpenApiParameter.QUERY
            ),
            OpenApiParameter(
                name="month", description="월(1~12)", required=False, type=int, location=OpenApiParameter.QUERY
            ),
        ],
        responses={200: OpenApiResponse(response=MonthlyLedgerStatsResponseSerializer, description="OK")},
        tags=["LedgerReport"],
    )
    def get(self, request, club_pk: int, ledger_pk: int):
        # 장부-클럽 소속 검증
        get_object_or_404(Ledger, pk=ledger_pk, club_id=club_pk)

        # year/month 파싱
        year = request.query_params.get("year")
        month = request.query_params.get("month")
        try:
            year = int(year) if year is not None else None
            month = int(month) if month is not None else None
            if month is not None and not (1 <= month <= 12):
                return Response({"detail": "month는 1~12 사이여야 합니다."}, status=status.HTTP_400_BAD_REQUEST)
        except ValueError:
            return Response({"detail": "year와 month는 정수여야 합니다."}, status=status.HTTP_400_BAD_REQUEST)

        data = monthly_ledger_stats(ledger_id=ledger_pk, year=year, month=month)
        ser = MonthlyLedgerStatsResponseSerializer(data)
        return Response(ser.data, status=status.HTTP_200_OK)
