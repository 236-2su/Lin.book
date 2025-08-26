from django.shortcuts import get_object_or_404
from drf_spectacular.utils import OpenApiParameter, OpenApiResponse, extend_schema
from rest_framework import status
from rest_framework.response import Response
from rest_framework.views import APIView

from ledger.models import Ledger

from .models import LedgerReports
from .serializers import (
    LedgerReportsSerializer,
    MonthlyLedgerStatsResponseSerializer,
    YearlyLedgerStatsResponseSerializer,
)
from .services import monthly_ledger_stats, yearly_ledger_stats


class MonthlyReportView(APIView):
    @extend_schema(
        summary="월간 보고서 목록 조회",
        description="해당 장부의 특정 년/월에 생성된 모든 월간 보고서 버전들을 조회합니다.",
        parameters=[
            OpenApiParameter("club_pk", int, OpenApiParameter.PATH, description="클럽 ID"),
            OpenApiParameter("ledger_pk", int, OpenApiParameter.PATH, description="장부 ID"),
            OpenApiParameter("year", int, OpenApiParameter.QUERY, description="조회할 년도"),
            OpenApiParameter("month", int, OpenApiParameter.QUERY, description="조회할 월"),
        ],
        responses={200: OpenApiResponse(LedgerReportsSerializer(many=True), description="OK")},
        tags=["LedgerReport"],
    )
    def get(self, request, club_pk: int, ledger_pk: int):
        ledger = get_object_or_404(Ledger, pk=ledger_pk, club_id=club_pk)

        try:
            year = int(request.query_params.get("year"))
            month = int(request.query_params.get("month"))
            if not (1 <= month <= 12):
                return Response({"detail": "month는 1~12 사이여야 합니다."}, status=status.HTTP_400_BAD_REQUEST)
        except (ValueError, TypeError):
            return Response({"detail": "year와 month는 필수 정수 값입니다."}, status=status.HTTP_400_BAD_REQUEST)

        base_title = f"{ledger.club.name}_{month}월_보고서"
        reports = LedgerReports.objects.filter(ledger=ledger, title__startswith=base_title).order_by("title")

        serializer = LedgerReportsSerializer(reports, many=True)
        return Response(serializer.data)

    @extend_schema(
        summary="월간 보고서 생성",
        description="해당 장부의 특정 년/월에 대한 월간 보고서를 새로 생성합니다. 최신 버전으로 자동 생성됩니다.",
        request=None,  # Body is empty, params in URL
        parameters=[
            OpenApiParameter("club_pk", int, OpenApiParameter.PATH, description="클럽 ID"),
            OpenApiParameter("ledger_pk", int, OpenApiParameter.PATH, description="장부 ID"),
            OpenApiParameter("year", int, OpenApiParameter.QUERY, description="생성할 년도"),
            OpenApiParameter("month", int, OpenApiParameter.QUERY, description="생성할 월"),
        ],
        responses={201: OpenApiResponse(MonthlyLedgerStatsResponseSerializer, description="Created")},
        tags=["LedgerReport"],
    )
    def post(self, request, club_pk: int, ledger_pk: int):
        get_object_or_404(Ledger, pk=ledger_pk, club_id=club_pk)

        try:
            year = int(request.query_params.get("year"))
            month = int(request.query_params.get("month"))
            if not (1 <= month <= 12):
                return Response({"detail": "month는 1~12 사이여야 합니다."}, status=status.HTTP_400_BAD_REQUEST)
        except (ValueError, TypeError):
            return Response({"detail": "year와 month는 필수 정수 값입니다."}, status=status.HTTP_400_BAD_REQUEST)

        report_data = monthly_ledger_stats(ledger_id=ledger_pk, year=year, month=month)
        serializer = MonthlyLedgerStatsResponseSerializer(report_data)
        return Response(serializer.data, status=status.HTTP_201_CREATED)


class YearlyReportView(APIView):
    @extend_schema(
        summary="연간 보고서 목록 조회",
        description="해당 장부의 특정 년도에 생성된 모든 연간 보고서 버전들을 조회합니다.",
        parameters=[
            OpenApiParameter("club_pk", int, OpenApiParameter.PATH, description="클럽 ID"),
            OpenApiParameter("ledger_pk", int, OpenApiParameter.PATH, description="장부 ID"),
            OpenApiParameter("year", int, OpenApiParameter.QUERY, description="조회할 년도"),
        ],
        responses={200: OpenApiResponse(LedgerReportsSerializer(many=True), description="OK")},
        tags=["LedgerReport"],
    )
    def get(self, request, club_pk: int, ledger_pk: int):
        ledger = get_object_or_404(Ledger, pk=ledger_pk, club_id=club_pk)

        try:
            year = int(request.query_params.get("year"))
        except (ValueError, TypeError):
            return Response({"detail": "year는 필수 정수 값입니다."}, status=status.HTTP_400_BAD_REQUEST)

        base_title = f"{ledger.club.name}_{year}년_보고서"
        reports = LedgerReports.objects.filter(ledger=ledger, title__startswith=base_title).order_by("title")

        serializer = LedgerReportsSerializer(reports, many=True)
        return Response(serializer.data)

    @extend_schema(
        summary="연간 보고서 생성",
        description="해당 장부의 특정 년도에 대한 연간 보고서를 새로 생성합니다. 최신 버전으로 자동 생성됩니다.",
        request=None,  # Body is empty, params in URL
        parameters=[
            OpenApiParameter("club_pk", int, OpenApiParameter.PATH, description="클럽 ID"),
            OpenApiParameter("ledger_pk", int, OpenApiParameter.PATH, description="장부 ID"),
            OpenApiParameter("year", int, OpenApiParameter.QUERY, description="생성할 년도"),
        ],
        responses={201: OpenApiResponse(YearlyLedgerStatsResponseSerializer, description="Created")},
        tags=["LedgerReport"],
    )
    def post(self, request, club_pk: int, ledger_pk: int):
        get_object_or_404(Ledger, pk=ledger_pk, club_id=club_pk)

        try:
            year = int(request.query_params.get("year"))
        except (ValueError, TypeError):
            return Response({"detail": "year는 필수 정수 값입니다."}, status=status.HTTP_400_BAD_REQUEST)

        report_data = yearly_ledger_stats(ledger_id=ledger_pk, year=year)
        serializer = YearlyLedgerStatsResponseSerializer(report_data)
        return Response(serializer.data, status=status.HTTP_201_CREATED)
