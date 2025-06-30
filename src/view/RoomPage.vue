<template>
  <div v-if="stage == 1">
    <FrameworkDesignStage :stage="stage" @updateStage="updateStage"/>
  </div>
  <div v-else-if="stage == 2">
    <CompleteScriptStage />
  </div>
  <div v-else>
    <DirectionSelectionStage :stage="stage" @updateStage="updateStage"/>
  </div>
</template>

<script setup>
import { ref } from "vue";
import FrameworkDesignStage from "@/components/cooperate-stages/FrameworkDesignStage.vue";
import CompleteScriptStage from "@/components/cooperate-stages/CompleteScriptStage.vue";
import DirectionSelectionStage from "@/components/cooperate-stages/DirectionSelectionStage.vue";
import { setupWebSocket } from "@/stores/socket";
// import NarrativeWorkspace from "@/components/roles/NarrativeWorkspace.vue";

const stage = ref(0);
setupWebSocket();

const updateStage = (newStage) => {
  stage.value = newStage;
};
</script>

<style></style>
