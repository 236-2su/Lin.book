from rest_framework import serializers


class PingSerializer(serializers.Serializer):
    response = serializers.CharField()


class TestRestRequestSerializer(serializers.Serializer):
    message = serializers.CharField()


class TestRestResponseSerializer(serializers.Serializer):
    response = serializers.CharField()


class TestRestGetResponseSerializer(serializers.Serializer):
    message = serializers.CharField()
