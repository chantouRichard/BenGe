<template>
  <div v-if="stage == 0">
    <FrameworkDesignStage :stage="stage" @updateStage="updateStage"/>
  </div>
  <div v-else-if="stage == 1">
    <CompleteScriptStage />
  </div>
</template>

<script setup>
import { ref } from "vue";
import FrameworkDesignStage from "@/components/cooperate-stages/FrameworkDesignStage.vue";
import CompleteScriptStage from "@/components/cooperate-stages/CompleteScriptStage.vue";

const stage = ref(0);

const updateStage = (newStage) => {
  stage.value = newStage;
};
</script>

<style></style>
