package com.example.scansheet

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController

// Cores extraídas do código SwiftUI para fácil referência e modificação
val darkBlue = Color(0xFF264073)
val mediumBlue = Color(0xFF40598C)
val cardBlueGradientStart = Color.Blue.copy(alpha = 0.7f)
val cardBlueGradientEnd = Color.Cyan.copy(alpha = 0.6f)
val cardGreenGradientStart = Color.Green.copy(alpha = 0.7f)
val cardMintGradientEnd = Color(0xFF98FF98).copy(alpha = 0.6f) // Aproximação de Color.mint

@Composable
fun HomeView(navController: NavController) {
    // Box é o equivalente ao ZStack do SwiftUI, permitindo sobrepor componentes.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(darkBlue, mediumBlue),
                    start = Offset.Zero,
                    end = Offset.Infinite
                )
            )
    ) {
        // Padrão de grade no fundo, equivalente à GridPatternView
        GridPattern(
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.3f)
        )

        // Column é o equivalente ao VStack, organizando os itens verticalmente.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 60.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
        // Seção do Cabeçalho
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Ícone do App
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .shadow(10.dp, RoundedCornerShape(20.dp), clip = false)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(cardBlueGradientStart, cardBlueGradientEnd)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "App Icon",
                    tint = Color.White,
                    modifier = Modifier.size(35.dp)
                )
            }

            // Título e Subtítulo
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Scansheet",
                    color = Color.White,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            // Descrição
            Text(
                text = "Transform any table or spreadsheet photo into a fully editable Excel file with AI-powered precision",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp, // Equivalente ao lineSpacing
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Cartões de Ação
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .padding(bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ActionCard(
                icon = Icons.Default.Call,
                title = "Upload Spreadsheet",
                subtitle = "Via Camera or Gallery",
                gradientColors = listOf(cardBlueGradientStart, cardBlueGradientEnd),
                action = { navController.navigate("ficha_selection_screen") }
            )
            ActionCard(
                icon = Icons.Default.Share,
                title = "Share Sheets",
                subtitle = "Export and collaborate",
                gradientColors = listOf(cardGreenGradientStart, cardMintGradientEnd),
                action = { navController.navigate("export_results_screen") }
            )
        }
    }
    }
}

/**
 * Componente reutilizável para os cartões de ação, equivalente à ActionCard do SwiftUI.
 */
@Composable
fun ActionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    gradientColors: List<Color>,
    action: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(15.dp, RoundedCornerShape(20.dp), clip = false)
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(colors = gradientColors))
            .clickable(onClick = action)
            .padding(20.dp)
    ) {
        // Row é o equivalente ao HStack, organizando os itens horizontalmente.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Container do Ícone
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Conteúdo de Texto
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal
                )
            }

            // Seta
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/**
 * Desenha um padrão de grade no Canvas, equivalente à GridPatternView do SwiftUI.
 */
@Composable
fun GridPattern(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val spacing = 40.dp.toPx()
        val lineColor = Color.White.copy(alpha = 0.1f)
        val strokeWidth = 1.dp.toPx()

        // Linhas verticais
        for (x in generateSequence(0f) { it + spacing }.takeWhile { it <= size.width }) {
            drawLine(
                color = lineColor,
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = strokeWidth
            )
        }

        // Linhas horizontais
        for (y in generateSequence(0f) { it + spacing }.takeWhile { it <= size.height }) {
            drawLine(
                color = lineColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = strokeWidth
            )
        }
    }
}

/**
 * Preview para visualização no Android Studio.
 */
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun HomeViewPreview() {
    val navController = rememberNavController()
    HomeView(navController = navController)
}
