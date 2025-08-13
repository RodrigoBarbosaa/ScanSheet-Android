package com.example.scansheet

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
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
import java.net.URLEncoder
import java.nio.charset.StandardCharsets


@Composable
fun FichaSelectionScreen(navController: NavController) {
    // Lista de opções de ficha
    val fichaOptions = remember { listOf("Cadastro individual SUS", "Geral") }

    // Estado para guardar a opção selecionada
    var selectedFicha by remember { mutableStateOf(fichaOptions.first()) }

    // Fundo da tela
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(colors = listOf(darkBlue, mediumBlue))
            )
    ) {
        // Padrão de grade reutilizado
        GridPattern(modifier = Modifier.fillMaxSize().alpha(0.3f))

        // Conteúdo principa
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Seção do Cabeçalho
            Column(
                modifier = Modifier.padding(top = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Ícone
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .shadow(10.dp, RoundedCornerShape(20.dp))
                        .clip(RoundedCornerShape(20.dp))
                        .background(Brush.linearGradient(colors = listOf(cardBlueGradientStart, cardBlueGradientEnd))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Home,
                        contentDescription = "Ícone de Digitalização",
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }

                // Título e Descrição
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Digitalização",
                        color = Color.White,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Selecione o tipo de ficha que você deseja digitalizar para otimizar o processo de reconhecimento",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Seção do Seletor
            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text(
                    text = "Escolha a ficha para digitalização",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.fillMaxWidth()
                )

                // Lista de opções
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    fichaOptions.forEach { option ->
                        PickerOptionCard(
                            title = option,
                            isSelected = (selectedFicha == option),
                            action = { selectedFicha = option }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Botão de Continuar
            Button(
                onClick = {
                    // Navega para a próxima tela ao clicar
                    val tag: String
                    if (selectedFicha == "Cadastro individual SUS") {
                        tag = "ficha_cadastro_individual"
                    } else {
                        tag = "outros"
                    }
                    navController.navigate("upload_step_screen/$tag")
                },
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 50.dp)
                .height(56.dp)
                .shadow(15.dp, RoundedCornerShape(20.dp)),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            contentPadding = PaddingValues() // Remove padding interno do botão
            ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(cardBlueGradientStart, cardBlueGradientEnd)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = Color.White
                    )
                    Text(
                        text = "Continuar",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
        }
    }
}

@Composable
private fun PickerOptionCard(
    title: String,
    isSelected: Boolean,
    action: () -> Unit
) {
    val borderColor = Color.White.copy(alpha = if (isSelected) 0.4f else 0.2f)
    val borderWidth = if (isSelected) 2.dp else 1.dp
    val backgroundColor = Color.White.copy(alpha = if (isSelected) 0.2f else 0.1f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .border(width = borderWidth, color = borderColor, shape = RoundedCornerShape(16.dp))
            .clickable(onClick = action)
            .padding(20.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Indicador de seleção (círculo)
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                }
            }

            // Título da opção
            Text(
                text = title,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )

            // Ícone de checkmark
            if (isSelected) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "Selecionado",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun FichaSelectionScreenPreview() {
    // Para o preview, podemos passar um NavController vazio, pois não haverá navegação real.
    val navController = rememberNavController()
    FichaSelectionScreen(navController = navController)
}

