from django.db import models

from ledger.models import Ledger


class LedgerReports(models.Model):
    ledger = models.ForeignKey(Ledger, on_delete=models.CASCADE)
    title = models.CharField(max_length=100)
    content = models.JSONField()
