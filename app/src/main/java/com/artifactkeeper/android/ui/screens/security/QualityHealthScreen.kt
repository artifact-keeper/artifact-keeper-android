package com.artifactkeeper.android.ui.screens.security

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.artifactkeeper.client.models.GateResponse
import com.artifactkeeper.client.models.HealthDashboardResponse
import com.artifactkeeper.client.models.RepoHealthResponse

internal val GradeAColor = Color(0xFF52C41A)
internal val GradeBColor = Color(0xFF36CFC9)
internal val GradeCColor = Color(0xFFFAAD14)
internal val GradeDColor = Color(0xFFFA8C16)
internal val GradeFColor = Color(0xFFF5222D)

internal fun gradeColor(grade: String): Color = when (grade.uppercase()) {
    "A" -> GradeAColor
    "B" -> GradeBColor
    "C" -> GradeCColor
    "D" -> GradeDColor
    else -> GradeFColor
}

/**
 * Quality health overview: portfolio health dashboard (average score, grade
 * distribution, per-repo health) and the configured quality gates.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QualityHealthScreen(
    viewModel: QualityViewModel = hiltViewModel(),
) {
    val state by viewModel.healthState.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadHealth() }

    when {
        state.isLoading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        state.error != null -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = state.error ?: "Unknown error",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = { viewModel.loadHealth(refresh = true) }) {
                        Text("Retry")
                    }
                }
            }
        }
        else -> {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                state.dashboard?.let { dashboard ->
                    item { HealthDashboardCard(dashboard) }

                    if (dashboard.repositories.isNotEmpty()) {
                        item {
                            Text(
                                text = "Repository Health",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        items(dashboard.repositories, key = { it.repositoryId }) { repo ->
                            RepoHealthCard(repo)
                        }
                    }
                }

                if (state.gates.isNotEmpty()) {
                    item {
                        Text(
                            text = "Quality Gates (${state.gates.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    items(state.gates, key = { it.id }) { gate ->
                        QualityGateCard(gate)
                    }
                }
            }
        }
    }
}

@Composable
private fun HealthDashboardCard(dashboard: HealthDashboardResponse) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "Health Overview",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "${dashboard.totalArtifactsEvaluated} artifacts across ${dashboard.totalRepositories} repos",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = "${dashboard.avgHealthScore}",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        dashboard.avgHealthScore >= 80 -> GradeAColor
                        dashboard.avgHealthScore >= 60 -> GradeCColor
                        else -> GradeFColor
                    },
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GradePill("A", dashboard.reposGradeA, GradeAColor)
                GradePill("B", dashboard.reposGradeB, GradeBColor)
                GradePill("C", dashboard.reposGradeC, GradeCColor)
                GradePill("D", dashboard.reposGradeD, GradeDColor)
                GradePill("F", dashboard.reposGradeF, GradeFColor)
            }
        }
    }
}

@Composable
private fun GradePill(label: String, count: Long, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = "$label: $count",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = color,
        )
    }
}

@Composable
private fun RepoHealthCard(repo: RepoHealthResponse) {
    val color = gradeColor(repo.healthGrade)
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = repo.repositoryKey,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "${repo.artifactsPassing} passing, ${repo.artifactsFailing} failing of ${repo.artifactsEvaluated}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(color),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = repo.healthGrade.uppercase(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }
        }
    }
}

@Composable
private fun QualityGateCard(gate: GateResponse) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = gate.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                val statusColor = if (gate.isEnabled) GradeAColor else MaterialTheme.colorScheme.onSurfaceVariant
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(statusColor.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = if (gate.isEnabled) "Enabled" else "Disabled",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = statusColor,
                    )
                }
            }
            gate.description?.takeIf { it.isNotBlank() }?.let { desc ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Action: ${gate.action.replaceFirstChar { it.uppercase() }}  -  Checks: ${gate.requiredChecks.joinToString(", ")}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
