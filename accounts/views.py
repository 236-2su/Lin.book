import os
from datetime import date, datetime

import requests
from django.shortcuts import get_object_or_404
from drf_spectacular.utils import OpenApiResponse, extend_schema, extend_schema_view
from rest_framework import status, viewsets
from rest_framework.decorators import action
from rest_framework.exceptions import ValidationError
from rest_framework.response import Response

from club.models import User

from .models import Accounts, AccountTransactions
from .serializers import AccountCreateRequestSerializer, AccountSerializer


@extend_schema_view(
    list=extend_schema(
        summary="사용자의 계좌 목록 조회",
        responses={200: AccountSerializer(many=True)},
    ),
    retrieve=extend_schema(
        summary="사용자의 특정 계좌 상세 조회",
        responses={200: AccountSerializer},
    ),
    create=extend_schema(
        summary="사용자 계좌 생성",
        request=AccountCreateRequestSerializer,
        responses={201: AccountSerializer},
    ),
    destroy=extend_schema(
        summary="사용자 계좌 삭제",
        responses={204: OpenApiResponse(description="No Content")},
    ),
)
class AccountsViewSet(viewsets.ModelViewSet):
    queryset = Accounts.objects.select_related("user")
    serializer_class = AccountSerializer

    def get_queryset(self):
        return super().get_queryset().filter(user_id=self.kwargs["user_pk"])

    def perform_create(self, serializer):
        user = get_object_or_404(User, pk=self.kwargs["user_pk"])
        now_date = str(date.today().strftime("%Y%m%d"))
        now_time = str(datetime.now().strftime("%H%M%S"))

        def format_to_eight_digits(num):
            s = str(num)
            return s.zfill(8) if len(s) < 8 else s[-8:]

        last_account = Accounts.objects.last()
        last_pk = last_account.pk if last_account else 0

        try:
            response = requests.post(
                "https://finopenapi.ssafy.io/ssafy/api/v1/edu/demandDeposit/createDemandDepositAccount",
                json={
                    "Header": {
                        "apiName": "createDemandDepositAccount",
                        "transmissionDate": now_date,
                        "transmissionTime": now_time,
                        "institutionCode": "00100",
                        "fintechAppNo": "001",
                        "apiServiceCode": "createDemandDepositAccount",
                        "institutionTransactionUniqueNo": f"{now_date}{now_time[:4]}{format_to_eight_digits(last_pk + 2)}",
                        "apiKey": os.getenv("FINAPI_SECRET"),
                        "userKey": user.user_key,
                    },
                    "accountTypeUniqueNo": "001-1-2d2921541edf42",
                },
            )
            response.raise_for_status()

        except requests.exceptions.RequestException as e:
            raise ValidationError({"error": str(e)})

        payload = response.json()
        code = payload.get("REC", {}).get("accountNo")

        if not code:
            raise ValidationError({"error": "Failed to retrieve account number from external API."})

        serializer.save(user=user, code=code, amount=0)
