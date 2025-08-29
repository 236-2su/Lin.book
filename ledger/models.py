from django.db import models

from club.models import Club, ClubMember


class Ledger(models.Model):
    club = models.ForeignKey(Club, verbose_name=(""), on_delete=models.CASCADE)
    account = models.ForeignKey("accounts.Accounts", on_delete=models.SET_NULL, null=True, blank=True)
    name = models.CharField(max_length=50)
    created_at = models.DateField(auto_now_add=True)
    amount = models.IntegerField()
    admin = models.ForeignKey(ClubMember, on_delete=models.SET_NULL, null=True)


class Receipt(models.Model):
    image = models.ImageField(upload_to="receipts/", max_length=None)
    amount = models.IntegerField()
    date_time = models.DateTimeField(auto_now_add=True)
    items = models.JSONField()


class Event(models.Model):
    club = models.ForeignKey(Club, on_delete=models.CASCADE)
    name = models.CharField(max_length=100)
    start_date = models.DateField()
    end_date = models.DateField()
    description = models.TextField(null=True, blank=True)
    budget = models.IntegerField()


class LedgerTransactions(models.Model):
    ledger = models.ForeignKey(Ledger, on_delete=models.CASCADE)
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)  # 생성일, 수정일(변조 여부 확인)
    date_time = models.DateTimeField(auto_now=False, auto_now_add=False)  # 거래일(수정 가능)
    amount = models.IntegerField()
    type = models.CharField(max_length=50, null=True, blank=True)  #
    payment_method = models.CharField(max_length=50)  # 거래 방법(현금, 카드, 계좌이체, 기타등등)
    receipt = models.OneToOneField(Receipt, on_delete=models.SET_NULL, null=True, blank=True)  # 영수증 OCR 이미지
    description = models.TextField()
    vendor = models.CharField(max_length=100)
    event = models.ForeignKey(Event, on_delete=models.SET_NULL, null=True)
    # account_transaction = models.ForeignKey(AccountTransaction, on_delete=models.SET_NULL, null=True, blank=True)
