from drf_spectacular.utils import extend_schema_field
from rest_framework import serializers

from .models import Club, ClubMember, ClubWelcomePage, Dues


class ClubSerializer(serializers.ModelSerializer):
    image = serializers.ImageField(use_url=True, required=False)
    due = serializers.SerializerMethodField()

    class Meta:
        model = Club
        fields = "__all__"

    def get_due(self, obj):
        return obj.dues_set.first()


class ClubWelcomePageSerializer(serializers.ModelSerializer):
    short_description = serializers.SerializerMethodField()

    class Meta:
        model = ClubWelcomePage
        fields = ["image", "content", "short_description"]

    @extend_schema_field(serializers.CharField())
    def get_short_description(self, obj):
        return obj.club.short_description


class ClubCreateSerializer(serializers.ModelSerializer):
    admin = serializers.IntegerField(write_only=True)

    class Meta:
        model = Club
        fields = [
            "name",
            "department",
            "major_category",
            "minor_category",
            "description",
            "hashtags",
            "admin",
        ]


class ClubMemberCreateSerializer(serializers.ModelSerializer):
    class Meta:
        model = ClubMember
        fields = ["status", "role", "user"]


class ClubMemberSerializer(serializers.ModelSerializer):
    class Meta:
        model = ClubMember
        fields = "__all__"


class ClubLoginRequestSerializer(serializers.Serializer):
    email = serializers.EmailField()


class ClubLoginResponseSerializer(serializers.Serializer):
    pk = serializers.IntegerField()


class DueSerializer(serializers.ModelSerializer):
    paid = serializers.SerializerMethodField()
    member_name = serializers.SerializerMethodField()
    member_student_number = serializers.SerializerMethodField()
    charged_amount = serializers.SerializerMethodField()
    club_name = serializers.SerializerMethodField()
    club_id = serializers.SerializerMethodField()

    class Meta:
        model = Dues
        fields = [
            "description",
            "paid",
            "member_name",
            "member_student_number",
            "charged_amount",
            "club_name",
            "club_id",
        ]
        read_only_fields = ["paid_at", "due_to", "amount", "member"]

    def get_paid(self, obj):
        return obj.paid_at is not None

    def get_member_name(self, obj):
        return obj.member.user.name

    def get_member_student_number(self, obj):
        return obj.member.user.student_number

    def get_charged_amount(self, obj):
        return obj.amount

    def get_club_name(self, obj):
        return obj.club.name if obj.club is not None else "null"

    def get_club_id(self, obj):
        return obj.club.id if obj.club is not None else "null"


class DuesBatchClaimSerializer(serializers.Serializer):
    month = serializers.IntegerField(min_value=1, max_value=12, help_text="회비를 청구할 월")
    amount = serializers.IntegerField(min_value=1, help_text="청구할 회비 금액")


class DuePaySerializer(serializers.Serializer):
    user_id = serializers.IntegerField(help_text="회비를 납부하는 유저의 ID")
    month = serializers.IntegerField(min_value=1, max_value=12, help_text="납부 대상 월")
