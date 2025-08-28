from django.db import models

from club.models import Club
from user.models import User


class Accounts(models.Model):
    user = models.ForeignKey(User, on_delete=models.CASCADE)
    amount = models.IntegerField()
    code = models.CharField(max_length=50)
    created_at = models.DateField(auto_now_add=True)
    club = models.ForeignKey(Club, on_delete=models.SET_NULL, null=True, blank=True)


class AccountTransactions(models.Model):
    account = models.ForeignKey(Accounts, on_delete=models.CASCADE)
    occurred_at = models.DateTimeField(auto_now=False)
    amount = models.IntegerField()
    memo = models.TextField()
